#!/bin/bash

set -e -u -o pipefail

awslocal s3 mb s3://buurman-documents --region eu-west-1

awslocal s3api put-bucket-cors --bucket buurman-documents --cors-configuration '{
  "CORSRules": [
    {
      "AllowedOrigins": ["http://localhost:5173", "http://localhost:3000"],
      "AllowedMethods": ["GET", "HEAD"],
      "AllowedHeaders": ["*"],
      "MaxAgeSeconds": 3600
    }
  ]
}'

echo "S3 bucket 'buurman-documents' created with CORS policy"
