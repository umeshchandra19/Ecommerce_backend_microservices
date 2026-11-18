from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import cm
from reportlab.platypus import Image, PageBreak, Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle

ss = getSampleStyleSheet()
H1 = ParagraphStyle("H1", parent=ss["Heading1"], textColor=colors.HexColor("#1F3A5F"), fontSize=14, spaceBefore=10)
B = ParagraphStyle("B", parent=ss["BodyText"], fontSize=10, leading=14)
CELL = ParagraphStyle("c", parent=B, fontSize=9, leading=12)
NOTE = ParagraphStyle("N", parent=B, backColor=colors.HexColor("#FFF4D6"), borderPadding=6, spaceBefore=6, spaceAfter=8)
S = []
def p(t, st=B): S.append(Paragraph(t, st)); S.append(Spacer(1, 5))
def h(t): S.append(Paragraph(t, H1))
def c(t): return Paragraph(t, CELL)
def tbl(rows, widths):
    data = [[c(x) if not isinstance(x, Paragraph) else x for x in r] for r in rows]
    t = Table(data, colWidths=widths, hAlign="LEFT")
    t.setStyle(TableStyle([("BACKGROUND", (0,0), (-1,0), colors.HexColor("#DCE8F7")), ("GRID", (0,0), (-1,-1), 0.4, colors.grey), ("VALIGN", (0,0), (-1,-1), "TOP")]))
    S.append(t); S.append(Spacer(1, 8))

S.append(Paragraph("E-Commerce Backend Microservices", ParagraphStyle("T", parent=ss["Title"], textColor=colors.HexColor("#1F3A5F"))))
p("<b>Java, Spring Boot, Redis, JWT, PostgreSQL</b><br/>Author: Umesh Chandra Namani")
p("<b>About this report:</b> it explains the design and what the repository contains. Load-test figures (latency change under 1,000+ RPS) "
  "are intentionally left for the author to fill in from their own k6 runs; no numbers are invented here. The code was written and "
  "reviewed but not compiled in the environment used to prepare it, so run <font face='Courier'>mvn verify</font> first.", NOTE)

h("1. The idea in simple words")
p("When browsing, carts, stock and orders live in one program, a slow part can drag everything down. This project splits the "
  "shop into five small services. Each one has its own database and can fail without taking the others with it. "
  "Users log in once, get a token (JWT), and every service checks that token. A rate limiter stops one caller from flooding the system, "
  "and Redis keeps popular product pages in memory so they load faster.")
S.append(Image("docs/architecture.png", width=16.5*cm, height=16.5*cm*6.2/11)); S.append(Spacer(1, 6))

h("2. Services")
tbl([["Service", "Port / DB", "Responsibility"],
     ["auth-service", "8081 / authdb", "Register and login. Passwords stored as BCrypt hashes. Issues JWTs with user id, email and role."],
     ["catalog-service", "8082 / catalogdb", "Product data and stock. Cached reads. Atomically reserves or releases stock."],
     ["cart-service", "8083 / cartdb", "One cart per user: add, remove, clear."],
     ["order-service", "8084 / orderdb", "Stores orders with the price paid. Creating the same order twice returns the first one."],
     ["checkout-service", "8085 / none", "Runs the purchase steps across the other services and undoes stock changes if something fails."]],
    [3.3*cm, 3.2*cm, 10.3*cm])

h("3. How the main pieces work")
p("<b>JWT authentication.</b> A shared filter reads the Bearer token, checks signature and expiry, and makes the user id and role available to the controller. "
  "Product changes need the ADMIN role. Missing or bad tokens get 401.")
p("<b>Rate limiting.</b> For each caller (user id, or IP address if not logged in) a Redis counter is increased per minute. Over the limit gives 429 with a "
  "Retry-After header. If Redis is unreachable the limiter lets requests through, so a cache outage does not become a full outage.")
p("<b>Redis caching.</b> Single products are cached by id (5 minutes) and product lists for 30 seconds. Admin updates evict the entry. "
  "Stock changes evict the affected products only after the database commit, so an old value cannot be written back to the cache. "
  "Setting <font face='Courier'>CACHE_TYPE=none</font> turns caching off so the effect can be measured.")
p("<b>Safe stock handling.</b> Stock is reduced by one SQL statement that only succeeds when enough stock is left, so two buyers cannot both take the last item. "
  "Items are processed in id order so concurrent checkouts do not deadlock.")
S.append(Image("docs/checkout_flow.png", width=16.5*cm, height=16.5*cm*4.6/11)); S.append(Spacer(1, 4))
p("<b>Checkout and failures.</b> Every call to another service has a 1 s connect and 3 s read timeout and a circuit breaker. When a service keeps failing, the breaker opens "
  "and checkout answers 503 immediately instead of waiting. Retries are only used for safe read calls. If creating the order fails after stock was reserved, "
  "checkout first asks the order service whether the order was saved anyway; if not, it releases the stock.")

h("4. What was verified")
tbl([["Item", "Status"],
     ["Source files, YAML and compose files", "Written; every Java file passes a syntax-level compiler check; YAML files parse correctly"],
     ["Unit tests (JWT, rate limiter, internal key, checkout saga)", "Written; to be run with mvn verify (also set up in GitHub Actions)"],
     ["Full build and run with Docker", "Not run in the preparation environment (no Maven or Docker available); run docker compose up --build"],
     ["Load-test results and the 40% latency claim", "Not measured here; use loadtest/catalog-read.js and fill in the table below"]],
    [7.3*cm, 9.5*cm])

h("5. Load-test results (to fill in)")
p("Method: k6, constant 1,000 requests/s for 60 s on <font face='Courier'>GET /api/products/{id}</font>, once with <font face='Courier'>CACHE_TYPE=none</font> and once with Redis, "
  "same machine, rate limit raised, warm-up run discarded, each case repeated at least three times.")
tbl([["Case", "RPS", "p50 (ms)", "p95 (ms)", "p99 (ms)", "Errors"],
     ["No cache", "", "", "", "", ""], ["Redis cache", "", "", "", "", ""], ["Improvement", "", "", "", "", ""]],
    [4*cm, 2.4*cm, 2.6*cm, 2.6*cm, 2.6*cm, 2.6*cm])

h("6. Limits and next steps")
p("Payments are not implemented. The services share one JWT secret (a real system would use asymmetric keys). The rate limiter uses a fixed window. "
  "If a stock release fails during compensation it is only logged. Next steps: an API gateway, an outbox with a retry job, Flyway migrations, "
  "Testcontainers integration tests, and metrics and tracing.")
SimpleDocTemplate("docs/ShopMesh_Report.pdf", pagesize=A4, leftMargin=2*cm, rightMargin=2*cm, topMargin=2*cm, bottomMargin=2*cm,
                  title="E-Commerce Backend Microservices - Report", author="Umesh Chandra Namani").build(S)
