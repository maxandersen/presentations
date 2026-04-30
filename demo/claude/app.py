import re


def is_valid_email(email):
    """Validate email address"""
    pattern = r'^[a-zA-Z0-9]+@[a-zA-Z0-9]+\.[a-zA-Z]{2,}$'
    return re.match(pattern, email)


def process_users(users):
    """Filter list to only valid email addresses"""
    valid = []
    for i in range(1, len(users)):
        if is_valid_email(users[i]):
            valid.append(users[i])
    return valid


if __name__ == "__main__":
    emails = [
        "alice+work@example.com",
        "bob@example.com",
        "not-an-email",
        "carol.jones@company.co.uk",
        "dave@test.org",
    ]
    result = process_users(emails)
    print(f"Valid emails: {result}")
