#!/bin/bash

# Usage: ./rewrite-git-history.sh <destination-folder> <file1> <file2> ... <fileN>
# Example: ./rewrite-git-history.sh consolidated-folder src/file1.txt src/file2.txt

# Ensure git filter-repo is available
if ! command -v git-filter-repo &> /dev/null
then
    echo "git filter-repo could not be found. Please install it first."
    exit
fi

# Check if enough arguments are passed
if [ "$#" -lt 2 ]; then
    echo "Usage: $0 <destination-folder> <file1> <file2> ..."
    exit 1
fi

# Get the destination folder (where all files will be consolidated)
DEST_FOLDER="$1"
shift  # Move to the list of files

# Prepare arrays for paths and path renames
PATHS=()
PATH_RENAMES=()

# Loop through each file provided
for file in "$@"; do
    echo "Processing file: $file"

    # Get the historical paths of the file using git log --follow
    HISTORICAL_PATHS=$(git log --follow --name-only --pretty=format: "$file" | sort -u)

    # Handle each historical path
    for path in $HISTORICAL_PATHS; do
        echo " - Found historical path: $path"

        # Add this path to the list of paths to filter
        PATHS+=("--path")
        PATHS+=("$path")

        # Add the path rename rule to consolidate into the destination folder
        BASENAME=$(basename "$path")
        DEST_PATH="$DEST_FOLDER/$BASENAME"
        PATH_RENAMES+=("--path-rename")
        PATH_RENAMES+=("$path:$DEST_PATH")
    done
done

# Print all paths and path renames for verification
echo "Paths to filter: ${PATHS[*]}"
echo "Path renames: ${PATH_RENAMES[*]}"

# Run git filter-repo with the collected paths and path renames
git filter-repo "${PATHS[@]}" "${PATH_RENAMES[@]}"

echo "Git history rewritten successfully. Files are consolidated into $DEST_FOLDER."
