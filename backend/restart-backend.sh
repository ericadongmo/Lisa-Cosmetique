#!/bin/bash
# Nettoyage + redémarrage complet du backend sur le port 8080

echo "===== 1. Libération du port 8080 ====="
PIDS=$(netstat -ano 2>/dev/null | grep ":8080.*LISTENING" | awk '{print $5}')
if [ -z "$PIDS" ]; then
    echo "Port 8080 déjà libre"
else
    for PID in $PIDS; do
        echo "Tuer PID $PID (taskkill)"
        taskkill /F /PID "$PID" 2>/dev/null && echo "  → tué" || echo "  → taskkill échoué"
    done
    sleep 3
    if netstat -ano 2>/dev/null | grep -q ":8080.*LISTENING"; then
        echo "Port encore occupé — nettoyage via WMIC"
        for PID in $PIDS; do
            echo "Supprimer PID $PID via WMIC"
            wmic process where "ProcessId=$PID" delete 2>/dev/null && echo "  → OK" || echo "  → wmic échoué"
        done
        sleep 3
    fi
    if netstat -ano 2>/dev/null | grep -q ":8080.*LISTENING"; then
        echo "PORT OCCUPé — résiste malgré kill/wmic"
        exit 1
    fi
    echo "Port 8080 libéré ✓"
fi

echo ""
echo "===== 2. Lancement du backend ====="
cd lisa-cosmetique/backend
rm -f /tmp/backend_fresh.log
nohup ./mvnw -q spring-boot:run > /tmp/backend_fresh.log 2>&1 &
BACKEND_PID=$!
echo "Backend démarré, PID=$BACKEND_PID"

# Attendre que Tomcat écoute
echo "Attente du port 8080..."
for i in $(seq 1 45); do
    if netstat -ano 2>/dev/null | grep -q ":8080.*LISTENING"; then
        echo "Port 8080 écoute après ~$i secondes"
        break
    fi
    sleep 1
done

# Laisser le temps à Spring Boot de finaliser l'initialisation (JPA, Flyway, etc.)
sleep 12
echo "Initialisation Spring terminée (ou timeout)"

echo ""
echo "===== 3. Vérification health ====="
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v1/health 2>/dev/null)
if [ "$HTTP_CODE" = "200" ]; then
    echo "Health OK (HTTP 200)"
else
    echo "Health FAIL (HTTP $HTTP_CODE)"
    echo "=== Log backend (dernières lignes) ==="
    tail -n 30 /tmp/backend_fresh.log | grep -viE "^\s*$"
    exit 1
fi

echo ""
echo "===== 4. Test produits ====="
PRODUCTS_HTTP=$(curl -s -o /tmp/products_response.json -w "%{http_code}" http://localhost:8080/api/v1/products 2>/dev/null)
if [ "$PRODUCTS_HTTP" = "200" ]; then
    echo "Products OK (HTTP 200)"
    echo "=== Réponse produits ==="
    python3 -m json.tool /tmp/products_response.json 2>/dev/null | head -60 || cat /tmp/products_response.json | head -c 1000
else
    echo "Products FAIL (HTTP $PRODUCTS_HTTP)"
    echo "=== Réponse brute ==="
    cat /tmp/products_response.json 2>/dev/null | head -c 500
    echo ""
    echo "=== Log backend (erreurs) ==="
    tail -n 50 /tmp/backend_fresh.log | grep -iE "error|exception|products|500|failed" | grep -viE "^\s*$" | head -20
    exit 1
fi

echo ""
echo "===== 5. Lien Swagger ====="
echo "Swagger UI:  http://localhost:8080/swagger-ui/index.html"
echo "API docs:    http://localhost:8080/api-docs"
