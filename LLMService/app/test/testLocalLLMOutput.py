# test_query_expansion.py

from app.Service.QueryExpansionService import query_expansion_service


products = [
    "AI marketing tool",
    "AI copywriting tool",
    "AI calorie tracker",
    "cyber security product",
    "sports shirt",
    "protein supplement",
    "SEO tool",
    "CRM software",
    "mobile puzzle game",
    "social media scheduling tool",
    "email marketing tool",
    "website builder",
    "expense tracking app",
    "language learning app",
    "sleep tracking app",
    "fitness coaching app",
]


for products in products:
    print("\n" + "=" * 60)
    print(f"NICHE: {products}")
    print("=" * 60)

    queries = query_expansion_service.generate_search_queries(
        niche=products,
    )

    for i, query in enumerate(queries, start=1):
        print(f"{i}. {query}")