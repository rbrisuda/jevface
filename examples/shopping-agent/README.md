# Example: product review insights (plain Java)

Uses Jevface without Spring. [`ProductReview`](src/main/java/sk/rbr/jevface/examples/shopping/ProductReview.java)
asks five questions about each review. [`ReviewInsights`](src/main/java/sk/rbr/jevface/examples/shopping/ReviewInsights.java)
evaluates a batch of reviews, drops suspicious ones and aggregates the rest.

```bash
export TYPESAFE_API_KEY=...
./gradlew :examples:shopping-agent:run                               # built-in sample reviews
./gradlew :examples:shopping-agent:run --args="'Great fit' 'Too small, returned it'"
```

```
Reviews analysed: 5 (1 flagged as suspicious and ignored)
Average rating:   3.0 stars
Would buy again:  50%
Topics:           {QUALITY=1, SIZE_FIT=2, DELIVERY=1}
Size issues:      {too-large=1, too-small=1}
```
