kubectx dev-gcp
kubectl exec --tty deployment/bidrag-arbeidsflyt printenv | grep -E 'AZURE_|_URL|SCOPE|SRV|NAIS_APP_NAME|TOPIC|KAFKA' > src/test/resources/application-lokal-nais-secrets.properties