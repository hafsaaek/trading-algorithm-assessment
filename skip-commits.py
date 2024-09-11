def skip_commits_callback(commit, metadata):
    # List of commit hashes you want to skip
    commits_to_skip = {
        "abcdef1234567890abcdef1234567890abcdef12",  # Replace with actual commit hashes
        "1234567890abcdef1234567890abcdef12345678",
    }

    # Skip this commit if its hash is in the list
    if commit.original_id in commits_to_skip:
        commit.ignore = True
