recommenders-v2
===============

temporary git repository for sketching recommenders 2.0 APIs
Currently some projects still contain errors (*.calls, *.calls.rcp, *.feature.calls.rcp, *.feature.jayes , *.feature.models.rcp).
You can just close these projects.

Build information:

This projects needs some artifacts of the Code Recommenders Project. So you have to follow the following steps:

1. Checkout the Code Recommenders master branch (git fetch git://git.eclipse.org/gitroot/recommenders/org.eclipse.recommenders master && git checkout FETCH_HEAD)
2. Build the Code Recommender projects with "mvn clean install"
3. Now you can build this projects with "mvn clean install -P e43"