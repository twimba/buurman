#!/bin/sh

set -e

ENDPOINT="http://seaweedfs:8333"
BUCKET="buurman-documents"

echo "Waiting for SeaweedFS S3 gateway..."
until aws --endpoint-url "${ENDPOINT}" s3 ls 2>/dev/null; do
  sleep 2
done

echo "Creating S3 bucket '${BUCKET}'..."
aws --endpoint-url "${ENDPOINT}" s3 mb "s3://${BUCKET}" 2>/dev/null || true
echo "S3 bucket '${BUCKET}' ready"
