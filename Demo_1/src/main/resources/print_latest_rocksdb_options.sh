#!/bin/sh

echo "Printing latest RocksDB options for each task..."

if [ -z "$APPLICATION_ID" ]; then
  echo "Environment variable 'APPLICATION_ID' is required but not set"
  exit 1
fi

if [ -z "$STORE_NAME" ]; then
  echo "Environment variable 'STORE_NAME' is required but not set"
  exit 1
fi

BASE_DIR=/tmp/kafka-streams/"$APPLICATION_ID"
for store in "$BASE_DIR"/*/rocksdb/"$STORE_NAME"; do
  [ -d "$store" ] || continue
  latest_num=-1
  latest_file=""
  for f in "$store"/OPTIONS-*; do
    [ -e "$f" ] || continue
    suffix=${f##*/OPTIONS-}
    case $suffix in
      ''|*[!0-9]*)
        continue
        ;;
      *)
        if [ $suffix -gt $latest_num ]; then
          latest_num=$suffix
          latest_file=$f
        fi
        ;;
    esac
  done
  if [ -n "$latest_file" ]; then
    echo "=== Latest config for $store (OPTIONS-$latest_num) ==="
    cat "$latest_file"
    echo "=== End of config ==="
    echo
  else
    echo "No OPTIONS-* files found in $store"
    echo
  fi
done
