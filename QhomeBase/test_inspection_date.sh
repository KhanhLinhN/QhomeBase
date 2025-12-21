#!/bin/bash
# Script để test API inspectionDate
# Usage: ./test_inspection_date.sh {contractId} {token}

CONTRACT_ID=$1
TOKEN=$2

if [ -z "$CONTRACT_ID" ]; then
    echo "Usage: ./test_inspection_date.sh {contractId} [token]"
    echo "Example: ./test_inspection_date.sh 123e4567-e89b-12d3-a456-426614174000"
    exit 1
fi

API_URL="http://localhost:8080/api/contracts/$CONTRACT_ID"

echo "Testing API: $API_URL"
echo ""

if [ -z "$TOKEN" ]; then
    echo "⚠️  No token provided, testing without authentication..."
    RESPONSE=$(curl -s "$API_URL")
else
    echo "✅ Using provided token..."
    RESPONSE=$(curl -s -H "Authorization: Bearer $TOKEN" "$API_URL")
fi

# Check if response contains inspectionDate
if echo "$RESPONSE" | grep -q "inspectionDate"; then
    echo "✅ SUCCESS: API response contains 'inspectionDate' field"
    echo ""
    echo "inspectionDate value:"
    echo "$RESPONSE" | grep -o '"inspectionDate":"[^"]*"' | head -1
else
    echo "❌ ERROR: API response does NOT contain 'inspectionDate' field"
    echo ""
    echo "Response preview:"
    echo "$RESPONSE" | head -20
fi

echo ""
echo "Full response (formatted):"
echo "$RESPONSE" | python -m json.tool 2>/dev/null || echo "$RESPONSE"





