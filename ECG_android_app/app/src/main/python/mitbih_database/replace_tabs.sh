#!/bin/bash
for file in *.txt; do
    if [ -f "$file" ]; then
        # Replace tabs with spaces on the first line of the file
        sed -i '' '1s/\t/ /g' "$file"
        sed -i '' '1s/Sample #/Sample_#/g' "$file"
    fi
done

