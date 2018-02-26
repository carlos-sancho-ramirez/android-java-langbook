#!/bin/sh -e
# Is this not a build which was triggered by setting a new tag?
if [ -z "$TRAVIS_TAG" ]; then
  echo -e "Tagging the commit.\n"

  git config --global user.email "travis@travis-ci.org"
  git config --global user.name "Travis"

  # Add tag and push to master.
  git tag -a v1.0.0-cibuild${TRAVIS_BUILD_NUMBER} -m "Travis build $TRAVIS_BUILD_NUMBER pushed a tag."
  git push origin --tags
  git fetch origin
fi
