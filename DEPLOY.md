# Continuous deployment: Kubernetes + Argo CD

```
push to main → CI (build, sast, dast, docker) → release job commits new image tag
            → Argo CD (in your cluster) sees the commit → rolls out the new pods
```

## What's in the repo

| Path | Purpose |
|---|---|
| `k8s/kafka.yaml` | Kafka: a StatefulSet with a 1 GB disk, plus a Service at `kafka:9092` |
| `k8s/app.yaml` | The app: 2 replicas, rolling updates, a Service on `localhost:8081` |
| `k8s/kustomization.yaml` | Ties both together; holds the image tag the pipeline updates |
| `argocd/application.yaml` | Tells Argo CD to watch `k8s/` on `main` |
| `.github/workflows/maven.yml` | New `release` job at the end |

## One-time setup

### 0. Before you start
- Docker Desktop → Settings → Kubernetes → **Enable Kubernetes**.
- Docker Desktop → Settings → Resources: give it at least **6 GB memory**.
- Stop the compose stack, so port 8081 is free: `docker compose down`.
- `kubectl config current-context` should say `docker-desktop`.

### 1. Make the repo and image readable
Argo CD and Kubernetes must be able to read them without a password.
- **Repo:** public (simplest).
- **Image:** GitHub profile → Packages → kafka-docker-demo → Package settings → Change visibility → Public.

Keeping the image private instead? Create a pull secret (token needs `read:packages`),
then uncomment `imagePullSecrets` in `k8s/app.yaml`:
```
kubectl create namespace kafka-demo
kubectl create secret docker-registry ghcr-pull -n kafka-demo \
  --docker-server=ghcr.io --docker-username=gopu-chandra --docker-password=<token>
```

### 2. Allow the pipeline to commit
Repo → Settings → Actions → General → Workflow permissions → **Read and write permissions** → Save.

### 3. Install Argo CD
```
kubectl create namespace argocd
kubectl apply -n argocd --server-side --force-conflicts \
  -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
kubectl get pods -n argocd -w        # wait until all are Running, then Ctrl+C
```

### 4. Open the Argo CD UI
```
kubectl port-forward svc/argocd-server -n argocd 8443:443
```
Open https://localhost:8443 (accept the browser warning; it's a self-signed certificate).
- User: `admin`
- Password:
  ```
  kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d; echo
  ```
(8443 is used because 8080 belongs to the Kafka UI.)

### 5. Register the app with Argo CD
```
kubectl apply -f argocd/application.yaml
```
Argo CD creates the `kafka-demo` namespace, starts Kafka, then the app.

### 6. Check it
```
kubectl get pods -n kafka-demo
curl -X POST http://localhost:8081/orders -H "Content-Type: application/json" \
  -d '{"product":"Laptop","quantity":1,"price":999.99}'
kubectl logs -n kafka-demo -l app=kafka-demo-app -f --prefix
```

## Day to day: watch a release happen
1. Change some code (e.g. a log message in `OrderConsumer`) and push to `main`.
2. The pipeline runs; the last job commits `deploy: ... sha-xxxxxxx [skip ci]`.
3. Argo CD checks Git about every 3 minutes (or click **Refresh** in the UI).
4. Watch the pods roll over one at a time: `kubectl get pods -n kafka-demo -w`

## Try the GitOps behaviour
- **Self-heal:** `kubectl scale deployment kafka-demo-app -n kafka-demo --replicas=5`
  → Argo CD sets it back to 2.
- **Rollback:** revert the `deploy:` commit in Git → Argo CD rolls back to the previous image.

## Kafka commands inside the cluster
```
kubectl exec -it -n kafka-demo kafka-0 -- /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
kubectl exec -it -n kafka-demo kafka-0 -- /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group order-service
```

## Clean up
```
kubectl delete -f argocd/application.yaml     # Argo CD removes the app (prune)
kubectl delete namespace kafka-demo           # also removes Kafka's disk
kubectl delete namespace argocd               # removes Argo CD
```
