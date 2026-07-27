# Deploying Helios on Kubernetes

These manifests deploy the full stack — **MySQL**, the **Spring Boot backend**, and the
**React/nginx frontend** — to any Kubernetes cluster. They mirror the `docker-compose.yml` setup.

```
k8s/
├── 00-namespace.yaml        # helios namespace
├── 01-mysql-secret.yaml     # DB credentials (Secret)
├── 02-mysql.yaml            # MySQL StatefulSet + PVC + headless Service
├── 03-backend-config.yaml   # backend ConfigMap (non-secret) + Secret (JWT, etc.)
├── 04-backend.yaml          # backend Deployment (2 replicas) + ClusterIP Service
├── 05-frontend.yaml         # frontend Deployment (2 replicas) + ClusterIP Service
├── 06-ingress.yaml          # Ingress → frontend (nginx proxies /api to backend)
└── kustomization.yaml       # applies everything in order
```

## Architecture in-cluster

```
            Ingress (helios.local)
                    │
                    ▼
            frontend (nginx :80)  ──/api,/actuator──►  backend (:8080)
                                                              │
                                                              ▼
                                                       mysql (:3306, PVC)
```

The frontend's nginx already proxies `/api` and `/actuator` to `http://backend:8080`, so the
Ingress only routes to the frontend. **Keep the backend Service named `backend`** or the proxy breaks.

---

## Prerequisites

- A running cluster: **kind**, **minikube**, Docker Desktop, or a cloud cluster (EKS/GKE/AKS).
- `kubectl` configured for that cluster.
- An **ingress controller** (for `06-ingress.yaml`). For kind/minikube, install ingress-nginx:
  ```bash
  kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/cloud/deploy.yaml
  ```

## 1. Build the images

```bash
# from the repo root
docker build -t helios-backend:latest ./backend
docker build -t helios-frontend:latest ./frontend
```

## 2. Make the images available to the cluster

The manifests use `imagePullPolicy: IfNotPresent` with local tags. Load them into your local cluster:

**kind:**
```bash
kind load docker-image helios-backend:latest helios-frontend:latest
```

**minikube:**
```bash
minikube image load helios-backend:latest
minikube image load helios-frontend:latest
```

**Remote/cloud cluster:** push to a registry instead and update the `image:` fields, e.g.
`docker tag helios-backend:latest <registry>/helios-backend:1.0.0 && docker push ...`.

## 3. Deploy

```bash
kubectl apply -k k8s/
```

Watch it come up:
```bash
kubectl -n helios get pods -w
```

Expected order: `mysql` becomes Ready → backend init container passes → `backend` Ready → `frontend` Ready.

## 4. Access the app

**Via Ingress** — add a hosts entry, then browse to `http://helios.local:8000`:
```bash
echo "127.0.0.1 helios.local" | sudo tee -a /etc/hosts
```
(The kind cluster maps host ports **8000→80** and **8443→443**, because ports 80/443 are used by
the workstation. Requires an ingress controller installed in the cluster.)

**Or via port-forward (no ingress needed):**
```bash
kubectl -n helios port-forward svc/frontend 8081:80     # http://localhost:8081
kubectl -n helios port-forward svc/backend 8080:8080    # http://localhost:8080/swagger-ui.html
```

Default seeded admin login: `admin@hellodoctor.local` / `Admin@12345`.

---

## Operating

**Scale the stateless backend:**
```bash
kubectl -n helios scale deployment/backend --replicas=4
```

**Roll out a new image version:**
```bash
kubectl -n helios set image deployment/backend backend=helios-backend:1.1.0
kubectl -n helios rollout status deployment/backend
```

**Inspect the database:**
```bash
kubectl -n helios exec -it statefulset/mysql -- mysql -uhelios -phelios helios -e "SELECT role, COUNT(*) FROM users GROUP BY role;"
```

**Logs:**
```bash
kubectl -n helios logs deployment/backend -f
```

**Tear down (keeps nothing):**
```bash
kubectl delete -k k8s/
# the MySQL PVC is deleted with the namespace; to keep data, back it up first
```

---

## Production hardening (recommended next steps)

- **Secrets:** don't commit real values. Use `kubectl create secret`, Sealed Secrets, or the
  External Secrets Operator. Rotate `HELIOS_JWT_SECRET` and DB passwords.
- **Disable seeding:** set `SEED_ADMIN_ENABLED=false` and `SEED_SAMPLE_ENABLED=false` in
  `backend-config` for real environments; create the admin out-of-band.
- **Database:** prefer a managed DB (RDS/Cloud SQL) over an in-cluster StatefulSet, and switch schema
  management to Flyway/Liquibase instead of Hibernate auto-DDL.
- **TLS:** add cert-manager + a TLS block on the Ingress.
- **Autoscaling:** add a `HorizontalPodAutoscaler` for the backend (CPU/memory based).
- **Resource tuning:** adjust the `requests/limits` to match your load testing.
- **PodDisruptionBudget & anti-affinity:** spread replicas across nodes for availability.

---

## Troubleshooting (issues seen on first bring-up)

- **`kind load` → "no nodes found for cluster kind":** no cluster exists yet. Create one first:
  `kind create cluster --name helios --config k8s/kind-cluster.yaml`, then load with
  `--name helios`.
- **kind create fails: "failed to bind host port 0.0.0.0:80":** something already uses host port 80.
  This config maps ingress to **8000/8443** instead. Adjust `kind-cluster.yaml` if those clash too.
- **MySQL `CrashLoopBackOff` with "unknown variable 'default-authentication-plugin'":** MySQL **8.4
  removed** that option. It must NOT be passed as an arg (already removed from `02-mysql.yaml`). If the
  data volume was half-initialized by the crash, delete it and recreate:
  `kubectl -n helios delete statefulset mysql --cascade=foreground && kubectl -n helios delete pvc data-mysql-0`.
- **Backend stuck `0/1` / restarting even though it logs "Started HeliosApplication":** the
  `/actuator/health` aggregate was `DOWN` because the **mail health indicator** tried to reach a
  non-existent SMTP server. Disabled via `MANAGEMENT_HEALTH_MAIL_ENABLED=false` in `backend-config`.
- **Changed a ConfigMap but pods still use old values:** editing a ConfigMap does **not** restart
  pods. Run `kubectl -n helios rollout restart deployment/backend` to pick up the change.

