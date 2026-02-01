#!/bin/bash

set -e -u -o pipefail

awslocal s3 mb s3://buurman-documents --region eu-west-1

echo "S3 bucket 'buurman-documents' created successfully"
