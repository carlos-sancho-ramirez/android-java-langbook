#!/bin/sh -e
# Is this not a build which was triggered by setting a new tag?
if [ -z "$TRAVIS_TAG" ]; then
  echo "Tagging the commit."

  tagName="v1.0.0-cibuild"
  if git tag -d ${tagName} 2> /dev/null; then
    git config credential.helper "store --file=.git/credentials"
    echo "https://${GH_TOKEN}:@github.com" > .git/credentials
    echo "Set GitHub OAuth token to push to remote"

    git push origin :${tagName}
    echo "Tag ${tagName} removed from remote"
  else
    echo "Tag ${tagName} not present"
  fi

  git tag ${tagName}
  echo "Created tag ${tagName}"

  git fetch origin
  echo "Fetched origin"
fi
