#!/bin/sh -e
# Is this not a build which was triggered by setting a new tag?
if [ -z "$TRAVIS_TAG" ]; then
  echo -e "Tagging the commit.\n"

  git config --global user.email "travis@travis-ci.org"
  git config --global user.name "Travis"
  echo "Git config updated for travis"

  # Add tag and push to master.
  git tag v1.0.0-cibuild${TRAVIS_BUILD_NUMBER}
  echo "Created tag v1.0.0-cibuild${TRAVIS_BUILD_NUMBER}"

  git fetch origin
  echo "Fetched origin"
fi
