#!/bin/sh -e
# Is this not a build which was triggered by setting a new tag?
if [ -z "$TRAVIS_TAG" ]; then
  echo "Tagging the commit."

  tagName="v1.0.0-cibuild"
  git tag -d ${tagName} 2> /dev/null || echo "Tag ${tagName} not present"
  git tag ${tagName}
  echo "Created tag ${tagName}"

  git fetch origin
  echo "Fetched origin"
fi
