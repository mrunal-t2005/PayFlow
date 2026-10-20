import {
  Activity,
  ArrowDownLeft,
  ArrowUpRight,
  Bell,
  ChevronDown,
  CircleHelp,
  CreditCard,
  Download,
  LayoutDashboard,
  Plus,
  Search,
  Settings,
  ShieldCheck,
  Wallet,
  X,
} from "lucide-react";
import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { useEffect, useMemo, useState } from "react";
import "./App.css";

type PaymentStatus = "PENDING" | "PROCESSING" | "SUCCEEDED" | "FAILED";

interface Payment {
  id: string;
  idempotencyKey: string;
  amount: number;
  currency: string;
  status: PaymentStatus;
  createdAt: string;
}

const API_URL = "http://localhost:8081/api/payments";

const navigation = [
  { label: "Overview", icon: LayoutDashboard },
  { label: "Payments", icon: CreditCard },
  { label: "Refunds", icon: Wallet },
  { label: "Analytics", icon: Activity },
];

function App() {
  const [activePage, setActivePage] = useState("Overview");
  const [search, setSearch] = useState("");
  const [payments, setPayments] = useState<Payment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [showNotifications, setShowNotifications] = useState(false);
  const [showProfile, setShowProfile] = useState(false);

  const [showCreatePayment, setShowCreatePayment] = useState(false);
  const [amount, setAmount] = useState("");
  const [currency, setCurrency] = useState("INR");
  const [creatingPayment, setCreatingPayment] = useState(false);
  const [paymentMessage, setPaymentMessage] = useState("");
  const [paymentMessageType, setPaymentMessageType] = useState<
    "success" | "error"
  >("success");

  useEffect(() => {
    const controller = new AbortController();

    async function loadPayments() {
      setLoading(true);
      setError("");

      try {
        const response = await fetch(API_URL, {
          headers: { Accept: "application/json" },
          signal: controller.signal,
        });

        if (!response.ok) {
          throw new Error(`Unable to load payments (HTTP ${response.status}).`);
        }

        const data: Payment[] = await response.json();

        if (!Array.isArray(data)) {
          throw new Error("The payment API returned an unexpected response.");
        }

        setPayments(data);
      } catch (err) {
        if (err instanceof Error && err.name === "AbortError") return;

        setError(
          err instanceof Error
            ? err.message
            : "Something went wrong while loading payments.",
        );
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    loadPayments();

    return () => controller.abort();
  }, []);

  const refreshPayments = async () => {
    const response = await fetch(API_URL, {
      headers: { Accept: "application/json" },
    });

    if (!response.ok) {
      throw new Error(`Unable to refresh payments (HTTP ${response.status}).`);
    }

    const data: Payment[] = await response.json();

    if (!Array.isArray(data)) {
      throw new Error("The payment API returned an unexpected response.");
    }

    setPayments(data);
  };

  const createPayment = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const numericAmount = Number(amount);

    if (!Number.isFinite(numericAmount) || numericAmount <= 0) {
      setPaymentMessageType("error");
      setPaymentMessage("Enter an amount greater than zero.");
      return;
    }

    if (!Number.isInteger(numericAmount * 100)) {
      setPaymentMessageType("error");
      setPaymentMessage("Enter an amount with no more than two decimal places.");
      return;
    }

    const idempotencyKey = crypto.randomUUID();

    setCreatingPayment(true);
    setPaymentMessage("");

    try {
      const response = await fetch(API_URL, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Accept: "application/json",
          "Idempotency-Key": idempotencyKey,
        },
        body: JSON.stringify({
          amount: numericAmount,
          currency,
        }),
      });

      if (!response.ok) {
        const responseText = await response.text();
        let message = `Payment creation failed (HTTP ${response.status}).`;

        if (responseText) {
          try {
            const parsed = JSON.parse(responseText);
            message = parsed.message || parsed.error || responseText;
          } catch {
            message = responseText;
          }
        }

        throw new Error(message);
      }

      setPaymentMessageType("success");
      setPaymentMessage("Payment created successfully.");
      setAmount("");
      setCurrency("INR");
      setShowCreatePayment(false);
      setActivePage("Payments");

      try {
        await refreshPayments();
      } catch {
        setPaymentMessage(
          "Payment was created, but the list could not refresh. Please reload the page.",
        );
      }
    } catch (err) {
      setPaymentMessageType("error");
      setPaymentMessage(
        err instanceof Error
          ? err.message
          : "Something went wrong while creating the payment.",
      );
    } finally {
      setCreatingPayment(false);
    }
  };

  const formatCurrency = (value: number, code = "INR") =>
    new Intl.NumberFormat("en-IN", {
      style: "currency",
      currency: code,
      maximumFractionDigits: 2,
    }).format(value);

  const formatDate = (date: string) => {
    const parsedDate = new Date(date);

    if (Number.isNaN(parsedDate.getTime())) return "—";

    return new Intl.DateTimeFormat("en-IN", {
      day: "2-digit",
      month: "short",
      year: "numeric",
    }).format(parsedDate);
  };

  const filteredPayments = useMemo(() => {
    const query = search.trim().toLowerCase();

    return payments
      .filter((payment) => {
        if (activePage === "Refunds") return false;

        return `${payment.id} ${payment.idempotencyKey} ${payment.status} ${payment.currency}`
          .toLowerCase()
          .includes(query);
      })
      .sort(
        (a, b) =>
          new Date(b.createdAt).getTime() -
          new Date(a.createdAt).getTime(),
      );
  }, [payments, search, activePage]);

  const succeeded = payments.filter(
    (payment) => payment.status === "SUCCEEDED",
  );

  const pending = payments.filter(
    (payment) =>
      payment.status === "PENDING" || payment.status === "PROCESSING",
  );

  const failed = payments.filter((payment) => payment.status === "FAILED");

  const totalVolume = payments.reduce(
    (sum, payment) => sum + payment.amount,
    0,
  );

  const successfulVolume = succeeded.reduce(
    (sum, payment) => sum + payment.amount,
    0,
  );

  const chartData = useMemo(() => {
    const grouped = new Map<string, number>();

    payments.forEach((payment) => {
      const day = new Date(payment.createdAt).toLocaleDateString("en-IN", {
        day: "2-digit",
        month: "short",
      });

      grouped.set(day, (grouped.get(day) ?? 0) + payment.amount);
    });

    return Array.from(grouped, ([day, volume]) => ({ day, volume })).reverse();
  }, [payments]);

  const exportPayments = () => {
    const escapeCsv = (value: string | number) =>
      `"${String(value).replace(/"/g, '""')}"`;

    const rows = [
      ["Payment ID", "Idempotency Key", "Date", "Currency", "Amount", "Status"],
      ...filteredPayments.map((payment) => [
        payment.id,
        payment.idempotencyKey || "",
        payment.createdAt,
        payment.currency,
        payment.amount,
        payment.status,
      ]),
    ];

    const csv = rows.map((row) => row.map(escapeCsv).join(",")).join("\n");
    const url = URL.createObjectURL(
      new Blob([csv], { type: "text/csv;charset=utf-8;" }),
    );
    const link = document.createElement("a");

    link.href = url;
    link.download = "payflow-payments.csv";
    link.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a
          className="brand"
          href="#"
          onClick={(event) => {
            event.preventDefault();
            setActivePage("Overview");
          }}
        >
          <span className="brand-mark">
            <Activity size={23} strokeWidth={2.5} />
          </span>
          <span className="brand-name">
            pay<span>flow</span>
          </span>
        </a>

        <div className="workspace-label">WORKSPACE</div>

        <button
          className="workspace-switcher"
          onClick={() => setShowProfile(!showProfile)}
        >
          <span className="workspace-avatar">P</span>
          <span className="workspace-copy">
            <strong>PayFlow Inc.</strong>
            <small>Business account</small>
          </span>
          <ChevronDown size={16} />
        </button>

        <div className="nav-label">PLATFORM</div>

        <nav className="navigation">
          {navigation.map(({ label, icon: Icon }) => (
            <button
              key={label}
              className={`nav-item ${activePage === label ? "active" : ""}`}
              onClick={() => setActivePage(label)}
            >
              <Icon size={18} strokeWidth={1.9} />
              <span>{label}</span>
              {label === "Payments" && (
                <span className="nav-count">{payments.length}</span>
              )}
            </button>
          ))}
        </nav>

        <div className="sidebar-bottom">
          <div className="secure-card">
            <div className="secure-icon">
              <ShieldCheck size={19} />
            </div>
            <strong>Your money, protected.</strong>
            <p>
              Monitor transactions and keep your payment operations organized.
            </p>
          </div>

          <button className="nav-item">
            <Settings size={18} />
            <span>Settings</span>
          </button>

          <button className="nav-item">
            <CircleHelp size={18} />
            <span>Help &amp; support</span>
          </button>

          <div className="sidebar-footer">
            <div className="profile-avatar">MR</div>
            <div className="profile-copy">
              <strong>Mrunal</strong>
              <small>Administrator</small>
            </div>
            <button
              className="icon-button profile-menu"
              onClick={() => setShowProfile(!showProfile)}
              aria-label="Profile menu"
            >
              <ChevronDown size={17} />
            </button>
          </div>

          {showProfile && (
            <div className="profile-popover">
              <strong>PayFlow Inc.</strong>
              <span>Local development workspace</span>
              <button onClick={() => setShowProfile(false)}>Close menu</button>
            </div>
          )}
        </div>
      </aside>

      <main className="main-area">
        <header className="topbar">
          <div className="breadcrumb">
            <span>Workspace</span>
            <span className="breadcrumb-slash">/</span>
            <strong>{activePage}</strong>
          </div>

          <div className="topbar-actions">
            <label className="global-search">
              <Search size={17} />
              <input
                aria-label="Search payments"
                placeholder="Search anything..."
                value={search}
                onChange={(event) => setSearch(event.target.value)}
              />
              <kbd>Ctrl K</kbd>
            </label>

            <button
              className={`icon-button notification-button ${showNotifications ? "selected" : ""}`}
              aria-label="Notifications"
              onClick={() => setShowNotifications(!showNotifications)}
            >
              <Bell size={19} />
              <span className="notification-dot" />
            </button>

            <button className="help-button" aria-label="Help">
              <CircleHelp size={18} />
            </button>
          </div>
        </header>

        {showNotifications && (
          <div className="notification-panel">
            <div className="notification-heading">
              <strong>Notifications</strong>
              <button
                className="icon-button"
                onClick={() => setShowNotifications(false)}
                aria-label="Close notifications"
              >
                <X size={17} />
              </button>
            </div>
            <p>
              {error
                ? "Payment API connection needs attention."
                : loading
                  ? "Loading your payments..."
                  : `${payments.length} payments loaded from your backend.`}
            </p>
            <small>PayFlow · Local workspace</small>
          </div>
        )}

        <div className="page-content">
          <section className="page-heading">
            <div>
              <div className="eyebrow">
                <span className="live-dot" /> YOUR BUSINESS AT A GLANCE
              </div>
              <h1>
                {activePage === "Overview"
                  ? "Good afternoon, Mrunal"
                  : activePage}
              </h1>
              <p className="page-subtitle">
                {activePage === "Overview"
                  ? "Here's what's happening with your payments today."
                  : `Explore your ${activePage.toLowerCase()} activity and performance.`}
              </p>
            </div>

            <button
              className="primary-button"
              onClick={() => {
                setPaymentMessage("");
                setShowCreatePayment(true);
              }}
            >
              <Plus size={17} />
              Create payment
            </button>
          </section>

          {paymentMessage && (
            <div
              role="status"
              style={{
                display: "flex",
                alignItems: "center",
                justifyContent: "space-between",
                gap: 12,
                padding: "13px 16px",
                marginBottom: 20,
                borderRadius: 12,
                border:
                  paymentMessageType === "success"
                    ? "1px solid #b7e4ce"
                    : "1px solid #f2b8b5",
                background:
                  paymentMessageType === "success" ? "#effaf4" : "#fff3f2",
                color:
                  paymentMessageType === "success" ? "#176b45" : "#a52d2d",
              }}
            >
              <span>{paymentMessage}</span>
              <button
                className="icon-button"
                aria-label="Dismiss message"
                onClick={() => setPaymentMessage("")}
              >
                <X size={16} />
              </button>
            </div>
          )}

          {showCreatePayment && (
            <section
              className="panel"
              aria-labelledby="create-payment-title"
              style={{
                padding: 24,
                marginBottom: 24,
                maxWidth: 620,
                border: "1px solid #e7e5f5",
              }}
            >
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "flex-start",
                  gap: 16,
                  marginBottom: 20,
                }}
              >
                <div>
                  <h2 id="create-payment-title">Create a payment</h2>
                  <p style={{ marginTop: 6 }}>
                    Create a payment record in your PayFlow backend.
                  </p>
                </div>
                <button
                  className="icon-button"
                  aria-label="Close create payment form"
                  onClick={() => setShowCreatePayment(false)}
                  disabled={creatingPayment}
                >
                  <X size={18} />
                </button>
              </div>

              <form onSubmit={createPayment}>
                <div
                  style={{
                    display: "grid",
                    gridTemplateColumns: "minmax(0, 1.5fr) minmax(110px, 1fr)",
                    gap: 16,
                    marginBottom: 20,
                  }}
                >
                  <label style={{ display: "grid", gap: 8 }}>
                    <span style={{ fontWeight: 600, fontSize: 14 }}>
                      Amount
                    </span>
                    <input
                      type="number"
                      min="0.01"
                      step="0.01"
                      required
                      value={amount}
                      onChange={(event) => setAmount(event.target.value)}
                      placeholder="e.g. 499.00"
                      disabled={creatingPayment}
                      style={{
                        width: "100%",
                        boxSizing: "border-box",
                        padding: "12px 14px",
                        border: "1px solid #dedee8",
                        borderRadius: 10,
                        font: "inherit",
                        background: "#fff",
                      }}
                    />
                  </label>

                  <label style={{ display: "grid", gap: 8 }}>
                    <span style={{ fontWeight: 600, fontSize: 14 }}>
                      Currency
                    </span>
                    <select
                      value={currency}
                      onChange={(event) => setCurrency(event.target.value)}
                      disabled={creatingPayment}
                      style={{
                        width: "100%",
                        boxSizing: "border-box",
                        padding: "12px 10px",
                        border: "1px solid #dedee8",
                        borderRadius: 10,
                        font: "inherit",
                        background: "#fff",
                      }}
                    >
                      <option value="INR">INR (₹)</option>
                      <option value="USD">USD ($)</option>
                      <option value="EUR">EUR (€)</option>
                      <option value="GBP">GBP (£)</option>
                    </select>
                  </label>
                </div>

                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    flexWrap: "wrap",
                    gap: 10,
                  }}
                >
                  <button
                    className="primary-button"
                    type="submit"
                    disabled={creatingPayment || !amount || Number(amount) <= 0}
                  >
                    {creatingPayment ? "Creating..." : "Create payment"}
                  </button>
                  <button
                    className="secondary-button"
                    type="button"
                    onClick={() => setShowCreatePayment(false)}
                    disabled={creatingPayment}
                  >
                    Cancel
                  </button>
                </div>

                <p style={{ marginTop: 16, fontSize: 12, color: "#77798b" }}>
                  A unique idempotency key is generated for each submission to
                  help prevent duplicate payment creation when the same request
                  is retried.
                </p>
              </form>
            </section>
          )}

          {error && (
            <section
              className="panel"
              style={{ padding: 20, marginBottom: 20 }}
            >
              <h2>Couldn't load payments</h2>
              <p>{error}</p>
              <p>
                Confirm that Spring Boot is running on port 8081 and that the
                frontend is allowed to access the API.
              </p>
              <button
                className="secondary-button"
                onClick={() => window.location.reload()}
              >
                Retry
              </button>
            </section>
          )}

          <section className="metrics-grid">
            <article className="metric-card">
              <div className="metric-top">
                <span>Total payment volume</span>
                <span className="metric-icon purple">
                  <Wallet size={19} />
                </span>
              </div>
              <div className="metric-value">
                {loading ? "Loading..." : formatCurrency(totalVolume)}
              </div>
              <div className="metric-foot">
                <span className="trend-neutral">All records</span>
                <span>loaded payments</span>
              </div>
              <div className="metric-sparkline purple-line">
                <span /><span /><span /><span /><span /><span /><span />
              </div>
            </article>

            <article className="metric-card">
              <div className="metric-top">
                <span>Successful payments</span>
                <span className="metric-icon green">
                  <ShieldCheck size={19} />
                </span>
              </div>
              <div className="metric-value">
                {loading ? "Loading..." : succeeded.length}
              </div>
              <div className="metric-foot">
                <span className="trend-positive">
                  <ArrowUpRight size={14} /> Succeeded
                </span>
                <span>{formatCurrency(successfulVolume)}</span>
              </div>
              <div className="metric-sparkline green-line">
                <span /><span /><span /><span /><span /><span /><span />
              </div>
            </article>

            <article className="metric-card">
              <div className="metric-top">
                <span>Pending payments</span>
                <span className="metric-icon orange">
                  <Activity size={19} />
                </span>
              </div>
              <div className="metric-value">
                {loading ? "Loading..." : pending.length}
              </div>
              <div className="metric-foot">
                <span className="trend-neutral">Needs review</span>
                <span>pending / processing</span>
              </div>
              <div className="metric-sparkline orange-line">
                <span /><span /><span /><span /><span /><span /><span />
              </div>
            </article>

            <article className="metric-card">
              <div className="metric-top">
                <span>Failed payments</span>
                <span className="metric-icon blue">
                  <ArrowDownLeft size={19} />
                </span>
              </div>
              <div className="metric-value">
                {loading ? "Loading..." : failed.length}
              </div>
              <div className="metric-foot">
                <span className="trend-neutral">Failed</span>
                <span>all records</span>
              </div>
              <div className="metric-sparkline blue-line">
                <span /><span /><span /><span /><span /><span /><span />
              </div>
            </article>
          </section>

          <section className="dashboard-grid">
            <article className="panel volume-panel">
              <div className="panel-heading">
                <div>
                  <h2>Payment volume</h2>
                  <p>Payment amounts grouped by transaction date.</p>
                </div>
                <span className="period-select">All dates</span>
              </div>

              <div className="chart-summary">
                <strong>
                  {loading ? "Loading..." : formatCurrency(totalVolume)}
                </strong>
                <span className="summary-period">across loaded payments</span>
              </div>

              <div className="chart-container">
                {chartData.length > 0 && (
                  <ResponsiveContainer width="100%" height="100%">
                    <AreaChart
                      data={chartData}
                      margin={{ top: 12, right: 10, left: 0, bottom: 0 }}
                    >
                      <defs>
                        <linearGradient
                          id="volumeGradient"
                          x1="0"
                          y1="0"
                          x2="0"
                          y2="1"
                        >
                          <stop
                            offset="0%"
                            stopColor="#7568e8"
                            stopOpacity={0.24}
                          />
                          <stop
                            offset="95%"
                            stopColor="#7568e8"
                            stopOpacity={0.015}
                          />
                        </linearGradient>
                      </defs>

                      <CartesianGrid
                        stroke="#ececf3"
                        strokeDasharray="4 5"
                        vertical={false}
                      />

                      <XAxis
                        dataKey="day"
                        tick={{ fill: "#8b8da1", fontSize: 12 }}
                        axisLine={false}
                        tickLine={false}
                        dy={12}
                      />

                      <YAxis
                        tickFormatter={(value: number) => `${value / 1000}k`}
                        tick={{ fill: "#8b8da1", fontSize: 11 }}
                        axisLine={false}
                        tickLine={false}
                        width={38}
                        tickCount={5}
                      />

                      <Tooltip
                        formatter={(value) => [
                          formatCurrency(Number(value)),
                          "Payment volume",
                        ]}
                      />

                      <Area
                        type="monotone"
                        dataKey="volume"
                        stroke="#7568e8"
                        strokeWidth={2.7}
                        fill="url(#volumeGradient)"
                        activeDot={{ r: 5, strokeWidth: 0 }}
                      />
                    </AreaChart>
                  </ResponsiveContainer>
                )}

                {!loading && chartData.length === 0 && (
                  <p>No payment data is available for the chart yet.</p>
                )}
              </div>
            </article>

            <article className="panel activity-panel">
              <div className="panel-heading">
                <div>
                  <h2>Payment health</h2>
                  <p>Current transaction status</p>
                </div>
                <span className="health-indicator">
                  <span /> Backend data
                </span>
              </div>

              <div className="health-total">
                <strong>{loading ? "..." : payments.length}</strong>
                <span>total transactions</span>
              </div>

              <div className="health-bar">
                <span
                  className="bar-success"
                  style={{
                    width: `${
                      payments.length
                        ? (succeeded.length / payments.length) * 100
                        : 0
                    }%`,
                  }}
                />
                <span
                  className="bar-pending"
                  style={{
                    width: `${
                      payments.length
                        ? (pending.length / payments.length) * 100
                        : 0
                    }%`,
                  }}
                />
                <span
                  className="bar-failed"
                  style={{
                    width: `${
                      payments.length
                        ? (failed.length / payments.length) * 100
                        : 0
                    }%`,
                  }}
                />
              </div>

              <div className="health-legend">
                <div>
                  <span className="legend-dot success" />
                  <span>Succeeded</span>
                  <strong>{succeeded.length}</strong>
                  <small>
                    {payments.length
                      ? `${((succeeded.length / payments.length) * 100).toFixed(1)}%`
                      : "0%"}
                  </small>
                </div>

                <div>
                  <span className="legend-dot pending" />
                  <span>Pending</span>
                  <strong>{pending.length}</strong>
                  <small>
                    {payments.length
                      ? `${((pending.length / payments.length) * 100).toFixed(1)}%`
                      : "0%"}
                  </small>
                </div>

                <div>
                  <span className="legend-dot failed" />
                  <span>Failed</span>
                  <strong>{failed.length}</strong>
                  <small>
                    {payments.length
                      ? `${((failed.length / payments.length) * 100).toFixed(1)}%`
                      : "0%"}
                  </small>
                </div>
              </div>

              <div className="health-note">
                <ShieldCheck size={16} />
                <span>Summary calculated from your payment records.</span>
              </div>
            </article>
          </section>

          <section className="panel transactions-panel">
            <div className="panel-heading transactions-heading">
              <div>
                <h2>
                  {activePage === "Refunds"
                    ? "Refunds"
                    : "Recent transactions"}
                </h2>
                <p>
                  {activePage === "Refunds"
                    ? "Refunds are not displayed in this payment list."
                    : "Real payment records returned by your backend."}
                </p>
              </div>

              <div className="table-actions">
                <label className="table-search">
                  <Search size={15} />
                  <input
                    value={search}
                    onChange={(event) => setSearch(event.target.value)}
                    placeholder="Filter payments"
                  />
                </label>

                <button
                  className="secondary-button"
                  onClick={exportPayments}
                  disabled={loading || filteredPayments.length === 0}
                >
                  <Download size={15} /> Export
                </button>
              </div>
            </div>

            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>TRANSACTION</th>
                    <th>DATE</th>
                    <th>PAYMENT METHOD</th>
                    <th>AMOUNT</th>
                    <th>STATUS</th>
                    <th />
                  </tr>
                </thead>

                <tbody>
                  {loading && (
                    <tr>
                      <td colSpan={6} className="empty-state">
                        Loading payments from your backend...
                      </td>
                    </tr>
                  )}

                  {!loading &&
                    activePage !== "Refunds" &&
                    filteredPayments.map((payment) => (
                      <tr key={payment.id}>
                        <td>
                          <div className="customer-cell">
                            <span className="customer-avatar">
                              <CreditCard size={17} />
                            </span>
                            <span>
                              <strong>
                                {payment.id.slice(0, 8).toUpperCase()}
                              </strong>
                              <small>{payment.id}</small>
                            </span>
                          </div>
                        </td>

                        <td className="date-cell">
                          {formatDate(payment.createdAt)}
                        </td>

                        <td>
                          <span className="payment-method">
                            <CreditCard size={16} />
                            {payment.currency}
                          </span>
                        </td>

                        <td className="amount-cell">
                          {formatCurrency(payment.amount, payment.currency)}
                        </td>

                        <td>
                          <span
                            className={`status-pill ${payment.status.toLowerCase()}`}
                          >
                            <span />
                            {payment.status}
                          </span>
                        </td>

                        <td>
                          <button
                            className="row-menu"
                            aria-label={`View payment ${payment.id}`}
                            title={`Idempotency key: ${payment.idempotencyKey}`}
                            onClick={() =>
                              window.alert(
                                `Payment ID: ${payment.id}\n` +
                                  `Status: ${payment.status}\n` +
                                  `Amount: ${formatCurrency(payment.amount, payment.currency)}\n` +
                                  `Idempotency key: ${payment.idempotencyKey || "Not provided"}`,
                              )
                            }
                          >
                            •••
                          </button>
                        </td>
                      </tr>
                    ))}

                  {!loading && activePage === "Refunds" && (
                    <tr>
                      <td colSpan={6} className="empty-state">
                        This table lists payments, not refunds. Refund API
                        integration will be handled separately.
                      </td>
                    </tr>
                  )}

                  {!loading &&
                    activePage !== "Refunds" &&
                    filteredPayments.length === 0 && (
                      <tr>
                        <td colSpan={6} className="empty-state">
                          {error
                            ? "Payments could not be loaded."
                            : "No payments match your search."}
                        </td>
                      </tr>
                    )}
                </tbody>
              </table>
            </div>

            <div className="table-footer">
              <span>
                Showing{" "}
                <strong>{loading ? "..." : filteredPayments.length}</strong>{" "}
                of <strong>{loading ? "..." : payments.length}</strong> payments
              </span>

              <div className="pagination">
                <button disabled>Previous</button>
                <button className="current-page">1</button>
                <button disabled>Next</button>
              </div>
            </div>
          </section>

          <footer className="app-footer">
            <span>© 2026 PayFlow, Inc.</span>
            <span>
              <span className="footer-status" />
              {error
                ? "API connection issue"
                : loading
                  ? "Loading payment data"
                  : "Connected to payment API"}
              <span className="footer-separator">·</span>
              Live backend data
            </span>
          </footer>
        </div>
      </main>
    </div>
  );
}

export default App;