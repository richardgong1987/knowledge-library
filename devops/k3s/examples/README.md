# k3s Lemonade Stand lab

Small manifests for the k3s part of the DevOps book. Each file shows **one** idea. The number at the start of a file name is the lesson that explains it.

| File | Lesson | What it creates |
| --- | --- | --- |
| `00-namespace.yaml` | 3 | Namespace `lemonade` |
| `04-pod.yaml` | 4 | A single NGINX pod `lemonade-pod` (port 8080) |
| `06-deployment.yaml` | 6 | Deployment `lemonade-web`, 3 replicas, serving the menu ConfigMap |
| `07-service.yaml` | 7 | ClusterIP Service `lemonade-web` (80 → 8080) |
| `07-service-nodeport.yaml` | 7 | NodePort Service on port 30080 |
| `08-ingress.yaml` | 8 | Ingress for `lemonade.localhost` via Traefik |
| `09-configmap.yaml` | 9 | ConfigMap `lemonade-menu` with `index.html` |
| `09-secret.yaml` | 9 | Secret `lemonade-payment` and a pod `cashier` that reads it |
| `10-storage.yaml` | 10 | PVC `lemonade-orders` (local-path) and a pod `order-logger` |
| `11-deployment-healthy.yaml` | 11 | The same Deployment as lesson 6, plus probes and resources |
| `11-hpa.yaml` | 11 | HorizontalPodAutoscaler for `lemonade-web` (needs metrics-server) |
| `12-job.yaml` | 12 | Job `squeeze-lemons`: 6 completions, 2 at a time |
| `12-cronjob.yaml` | 12 | CronJob `daily-sales-report` at 18:00 London time |
| `12-daemonset.yaml` | 12 | DaemonSet `node-thermometer`: one pod per node |
| `12-statefulset.yaml` | 12 | Headless Service and StatefulSet `ledger` with one volume per pod |
| `13-lemonade-chart/` | 13 | A tiny Helm chart |
| `13-helmchart.yaml` | 13 | A k3s `HelmChart` object that installs the public `podinfo` chart |
| `13-opening-hours.yaml` | 13 | A ConfigMap to drop into the k3s auto-deploy folder |

## Start a cluster

On Linux:

```bash
curl -sfL https://get.k3s.io | sh -
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml   # or use: sudo k3s kubectl …
```

On macOS or Windows, with Docker:

```bash
docker run -d --name k3s-lab --hostname k3s-lab --privileged \
  -p 6443:6443 -p 8081:80 -p 30080:30080 \
  rancher/k3s:v1.37.2-k3s1 server
docker cp k3s-lab:/etc/rancher/k3s/k3s.yaml ./k3s.yaml
export KUBECONFIG=$PWD/k3s.yaml
kubectl get nodes
```

Docker needs about 1 GB of free memory for this. If other containers already use most of Docker's memory, the API server becomes very slow.

## Run the lessons

From this folder:

```bash
kubectl apply -f 00-namespace.yaml
kubectl apply -f 04-pod.yaml                                  # lesson 4
kubectl apply -f 09-configmap.yaml -f 06-deployment.yaml      # lesson 6
kubectl apply -f 07-service.yaml -f 07-service-nodeport.yaml  # lesson 7
kubectl apply -f 08-ingress.yaml                              # lesson 8
curl -H 'Host: lemonade.localhost' http://localhost:8081/     # Docker lab; use the node IP on Linux
kubectl apply -f 09-secret.yaml                               # lesson 9
kubectl apply -f 10-storage.yaml                              # lesson 10
kubectl apply -f 11-deployment-healthy.yaml -f 11-hpa.yaml    # lesson 11
kubectl apply -f 12-job.yaml -f 12-cronjob.yaml -f 12-daemonset.yaml -f 12-statefulset.yaml
helm install lemonade ./13-lemonade-chart -n lemonade-helm --create-namespace
kubectl apply -f 13-helmchart.yaml
docker cp 13-opening-hours.yaml k3s-lab:/var/lib/rancher/k3s/server/manifests/opening-hours.yaml
```

`kubectl apply -f .` also works: it applies every file in this folder in name order. `11-deployment-healthy.yaml` then replaces lesson 6's Deployment with its richer version, which is the intended final state. The chart folder is skipped, because `-f` does not recurse without `-R`.

## Clean up

```bash
helm uninstall lemonade -n lemonade-helm
kubectl delete -f 13-helmchart.yaml
kubectl delete namespace lemonade lemonade-helm
docker rm -f k3s-lab      # Docker lab only
```
