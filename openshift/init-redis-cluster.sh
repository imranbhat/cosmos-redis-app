#!/bin/bash
# Script to initialize Redis Cluster in OpenShift

# 1. Wait for all 6 pods to be ready
echo "Waiting for all Redis pods to be ready..."
oc wait --for=condition=Ready pod -l app=redis-cluster --timeout=300s

# 2. Get IP addresses of all pods
IPS=$(oc get pods -l app=redis-cluster -o jsonpath='{range .items[*]}{.status.podIP}:6379 {end}')

# 3. Create the cluster
echo "Creating Redis Cluster with nodes: $IPS"
oc exec -it redis-cluster-0 -- redis-cli --cluster create $IPS --cluster-replicas 1 --cluster-yes

# 4. Verify cluster status
echo "Verifying cluster status..."
oc exec -it redis-cluster-0 -- redis-cli cluster info
