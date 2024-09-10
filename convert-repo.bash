#!/bin/bash

# Usage: ./rewrite-files-with-auto-branch.sh <destination-folder> <file1> <file2> ... <fileN>
# Example: ./rewrite-files-with-auto-branch.sh consolidated-folder src/file1.txt src/file2.txt

# Ensure git filter-repo is available
if ! command -v git-filter-repo &> /dev/null
then
    echo "git filter-repo could not be found. Please install it first."
    exit 1
fi

# Check if enough arguments are passed
if [ "$#" -lt 2 ]; then
    echo "Usage: $0 <destination-folder> <file1> <file2> ..."
    exit 1
fi

# Get the destination folder (where all files will be consolidated)
DEST_FOLDER="$1"
shift  # Move to the list of files

# Process each file
while [ "$#" -gt 0 ]; do
    FILE="$1"
    shift  # Move to the next file

    # Generate branch name from the file name (basename without path)
    BASENAME=$(basename "$FILE")
    BRANCH_NAME="branch_${BASENAME%.*}"  # Remove file extension for cleaner branch name

    # Create a new branch for this file
    echo "Creating branch '$BRANCH_NAME' for file '$FILE'..."
    git checkout -b "$BRANCH_NAME"

    # Prepare arrays for paths and path renames
    PATHS=()
    PATH_RENAMES=()

    # Get historical paths for the file
    HISTORICAL_PATHS=$(git log --follow --name-only --pretty=format: "$FILE" | sort -u)

    # Add each historical path to the list of paths to filter
    for path in $HISTORICAL_PATHS; do
        echo " - Found historical path: $path"

        # Add this path to the list of paths to filter
        PATHS+=("--path")
        PATHS+=("$path")

        # Add the path rename rule to consolidate into the destination folder
        FILE_BASENAME=$(basename "$path")
        DEST_PATH="$DEST_FOLDER/$FILE_BASENAME"
        PATH_RENAMES+=("--path-rename")
        PATH_RENAMES+=("$path:$DEST_PATH")
    done

    # Run git filter-repo for this file
    echo "Running git filter-repo on branch '$BRANCH_NAME' for file '$FILE'..."
    git filter-repo "${PATHS[@]}" "${PATH_RENAMES[@]}"

    # Push the new branch (if needed, use --force to rewrite history on remote)
    echo "Pushing branch '$BRANCH_NAME'..."
    git push origin "$BRANCH_NAME" --force
done

echo "All file-specific branches have been processed."
