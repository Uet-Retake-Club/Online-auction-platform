package com.auction.client.controllers;

import com.auction.client.utils.SceneNavigator;
import com.auction.client.utils.ToastNotification;
import com.auction.client.utils.TopNavUtils;
import com.auction.client.utils.TransactionUiHelper;
import com.auction.client.utils.UserSession;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * WalletController handles balances, deposits, withdrawals, and history.
 *
 * <p>Four tabs: Overview (balance + recent), Deposit, Withdraw, History.
 * All monetary values are in USD. Replace dummy data and balance constants
 * with WalletService calls when the backend is ready.
 */
public class WalletController implements Initializable {

  /** Dummy available balance — replace with WalletService.getBalance(). */
  private double currentAvailableBalance = 2450.00;

  /** Dummy holding amount — funds locked in active bids. */
  private double currentHoldingBalance = 780.00;

  /** Deposit transaction type constant. */
  private static final String TYPE_DEPOSIT = "deposit";

  /** Withdrawal transaction type constant. */
  private static final String TYPE_WITHDRAW = "withdraw";

  /** Hold transaction type constant. */
  private static final String TYPE_HOLD = "hold";

  /** Refund transaction type constant. */
  private static final String TYPE_REFUND = "refund";

  // ── FXML — Top nav ────────────────────────────────────────
  
  

  // ── FXML — Sidebar nav ────────────────────────────────────
  @FXML private Button navOverview;
  @FXML private Button navDeposit;
  @FXML private Button navWithdraw;
  @FXML private Button navHistory;

  // ── FXML — Overview tab ───────────────────────────────────
  @FXML private VBox overviewTab;
  @FXML private Label availableBalance;
  @FXML private Label holdingBalance;
  @FXML private Label totalBalance;
  @FXML private VBox recentTxContainer;

  // ── FXML — Deposit tab ────────────────────────────────────
  @FXML private VBox depositTab;
  @FXML private TextField depositAmountField;
  @FXML private Label depositAmountError;
  @FXML private ComboBox<String> depositMethodCombo;
  @FXML private Label depositCurrentBalance;
  @FXML private Label depositAfterBalance;

  // ── FXML — Withdraw tab ───────────────────────────────────
  @FXML private VBox withdrawTab;
  @FXML private TextField withdrawAmountField;
  @FXML private Label withdrawAmountError;
  @FXML private ComboBox<String> withdrawBankCombo;
  @FXML private Label withdrawAvailableBalance;
  @FXML private Label withdrawReceiveAmount;

  // ── FXML — History tab ────────────────────────────────────
  @FXML private VBox historyTab;
  @FXML private Label statDeposited;
  @FXML private Label statSpent;
  @FXML private VBox historyTxContainer;
  @FXML private VBox historyEmpty;
  @FXML private Button filterAll;
  @FXML private Button filterDeposit;
  @FXML private Button filterWithdraw;
  @FXML private Button filterHold;

  // ── State ─────────────────────────────────────────────────
  private Button activeNav;
  private Button activeFilter;

  // ── Dummy transaction data ────────────────────────────────
  // Columns: description, amount (signed), date, type
  private final List<String[]> allTransactions = new ArrayList<>(Arrays.asList(
    new String[]{"Đặt cọc — Vintage Rolex Watch",   "-$780.00",   "15/05/2026", TYPE_HOLD},
    new String[]{"Nạp tiền",                         "+$1,000.00", "14/05/2026", TYPE_DEPOSIT},
    new String[]{"Thanh toán — Nike Air Jordan 1",   "-$210.00",   "12/05/2026", TYPE_WITHDRAW},
    new String[]{"Hoàn tiền — Sony WH-1000XM5",      "+$190.00",   "05/05/2026", TYPE_REFUND}
  ));

  // ── Lifecycle ─────────────────────────────────────────────

  @Override
  public void initialize(final URL url, final ResourceBundle rb) {
    
    
    activeNav = navOverview;
    activeFilter = filterAll;
    setupDepositTab();
    setupWithdrawTab();
    loadOverview();
    loadHistory(null);
  }

  // ── Setup ─────────────────────────────────────────────────

  /** Populates the deposit form with initial values. */
  private void setupDepositTab() {
    depositMethodCombo.getItems().addAll(
        "Thẻ Visa **** 4242",
        "Thẻ Mastercard **** 8888",
        "Chuyển khoản ngân hàng"
    );
    depositMethodCombo.getSelectionModel().selectFirst();
    depositCurrentBalance.setText(formatMoney(currentAvailableBalance));
    depositAfterBalance.setText(formatMoney(currentAvailableBalance));
    depositAmountField.textProperty().addListener((obs, old, val) -> updateDepositPreview(val));
  }

  /** Populates the withdraw form with initial values. */
  private void setupWithdrawTab() {
    withdrawBankCombo.getItems().addAll(
        "Vietcombank **** 7890",
        "Techcombank **** 3456",
        "BIDV **** 1234"
    );
    withdrawBankCombo.getSelectionModel().selectFirst();
    withdrawAvailableBalance.setText(formatMoney(currentAvailableBalance));
    withdrawReceiveAmount.setText("$0.00");
    withdrawAmountField.textProperty().addListener((obs, old, val) -> updateWithdrawPreview(val));
  }

  /**
   * Updates the deposit "after balance" preview.
   *
   * @param val the current raw text in the deposit field
   */
  private void updateDepositPreview(final String val) {
    depositAmountError.setText("");
    try {
      final double amt = Double.parseDouble(val.replace(",", ""));
      depositAfterBalance.setText(formatMoney(currentAvailableBalance + amt));
    } catch (NumberFormatException e) {
      depositAfterBalance.setText(formatMoney(currentAvailableBalance));
    }
  }

  /**
   * Updates the withdraw "you receive" preview.
   *
   * @param val the current raw text in the withdraw field
   */
  private void updateWithdrawPreview(final String val) {
    withdrawAmountError.setText("");
    try {
      withdrawReceiveAmount.setText(formatMoney(Double.parseDouble(val)));
    } catch (NumberFormatException e) {
      withdrawReceiveAmount.setText("$0.00");
    }
  }

  // ── Overview ──────────────────────────────────────────────

  /** Loads balance values and the 3 most recent transactions. */
  private void loadOverview() {
    availableBalance.setText(formatMoney(currentAvailableBalance));
    holdingBalance.setText("Đang giữ: " + formatMoney(currentHoldingBalance));
    totalBalance.setText("Tổng: " + formatMoney(currentAvailableBalance + currentHoldingBalance));
    recentTxContainer.getChildren().clear();
    final int limit = Math.min(3, allTransactions.size());
    for (int i = 0; i < limit; i++) {
      recentTxContainer.getChildren().add(buildRow(allTransactions.get(i), i == limit - 1));
    }
    
    // Update forms text as well
    depositCurrentBalance.setText(formatMoney(currentAvailableBalance));
    updateDepositPreview(depositAmountField.getText());
    withdrawAvailableBalance.setText(formatMoney(currentAvailableBalance));
  }

  // ── Deposit / Withdraw confirm ────────────────────────────

  /**
   * Validates and processes the deposit form.
   * TODO: replace with WalletService.deposit() when backend is ready.
   */
  @FXML
  private void onConfirmDeposit() {
    final String raw = depositAmountField.getText().trim();
    if (raw.isEmpty()) {
      depositAmountError.setText("Vui lòng nhập số tiền");
      return;
    }
    final double amount;
    try {
      amount = Double.parseDouble(raw.replace(",", ""));
    } catch (NumberFormatException ex) {
      depositAmountError.setText("Số tiền không hợp lệ");
      return;
    }
    if (amount <= 0) {
      depositAmountError.setText("Số tiền phải lớn hơn 0");
      return;
    }
    if (amount < 10) {
      depositAmountError.setText("Số tiền tối thiểu là $10.00");
      return;
    }
    
    currentAvailableBalance += amount;
    final String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    allTransactions.add(0, new String[]{
        "Nạp tiền", "+" + formatMoney(amount), dateStr, TYPE_DEPOSIT
    });

    ToastNotification.show(
        depositTab, "Nạp " + formatMoney(amount) + " thành công!", ToastNotification.Type.SUCCESS);
    depositAmountField.clear();
    loadOverview();
    onTabOverview();
  }

  /**
   * Validates and processes the withdrawal form.
   * TODO: replace with WalletService.withdraw() when backend is ready.
   */
  @FXML
  private void onConfirmWithdraw() {
    final String raw = withdrawAmountField.getText().trim();
    if (raw.isEmpty()) {
      withdrawAmountError.setText("Vui lòng nhập số tiền");
      return;
    }
    final double amount;
    try {
      amount = Double.parseDouble(raw.replace(",", ""));
    } catch (NumberFormatException ex) {
      withdrawAmountError.setText("Số tiền không hợp lệ");
      return;
    }
    if (amount <= 0) {
      withdrawAmountError.setText("Số tiền phải lớn hơn 0");
      return;
    }
    if (amount > currentAvailableBalance) {
      withdrawAmountError.setText(
          "Vượt quá số dư khả dụng (" + formatMoney(currentAvailableBalance) + ")");
      return;
    }
    if (amount < 20) {
      withdrawAmountError.setText("Số tiền rút tối thiểu là $20.00");
      return;
    }
    
    currentAvailableBalance -= amount;
    final String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    allTransactions.add(0, new String[]{
        "Rút tiền", "-" + formatMoney(amount), dateStr, TYPE_WITHDRAW
    });

    ToastNotification.show(
        withdrawTab,
        "Yêu cầu rút " + formatMoney(amount) + " đã được gửi!",
        ToastNotification.Type.SUCCESS);
    withdrawAmountField.clear();
    loadOverview();
    onTabOverview();
  }

  // ── History + filter ──────────────────────────────────────

  /** Shows all transactions. */
  @FXML
  private void onFilterAll() {
    switchFilter(filterAll);
    loadHistory(null);
  }

  /** Filters to deposit transactions only. */
  @FXML
  private void onFilterDeposit() {
    switchFilter(filterDeposit);
    loadHistory(TYPE_DEPOSIT);
  }

  /** Filters to withdrawal transactions only. */
  @FXML
  private void onFilterWithdraw() {
    switchFilter(filterWithdraw);
    loadHistory(TYPE_WITHDRAW);
  }

  /** Filters to hold transactions only. */
  @FXML
  private void onFilterHold() {
    switchFilter(filterHold);
    loadHistory(TYPE_HOLD);
  }

  /**
   * Loads transactions into the history tab, optionally filtered by type.
   *
   * @param typeFilter transaction type to filter by, or null for all
   */
  private void loadHistory(final String typeFilter) {
    final List<String[]> data;
    if (typeFilter == null) {
      data = allTransactions;
    } else {
      data = allTransactions.stream()
          .filter(tx -> tx[3].equals(typeFilter))
          .collect(Collectors.toList());
    }
    historyTxContainer.getChildren().clear();
    if (data.isEmpty()) {
      historyEmpty.setVisible(true);
      historyEmpty.setManaged(true);
      historyTxContainer.setVisible(false);
    } else {
      historyEmpty.setVisible(false);
      historyEmpty.setManaged(false);
      historyTxContainer.setVisible(true);
      for (int i = 0; i < data.size(); i++) {
        historyTxContainer.getChildren().add(buildRow(data.get(i), i == data.size() - 1));
      }
    }
  }

  /**
   * Updates the active filter button styling.
   *
   * @param btn the filter button to activate
   */
  private void switchFilter(final Button btn) {
    if (activeFilter != null) {
      activeFilter.getStyleClass().remove("btn-primary");
      activeFilter.getStyleClass().add("btn-secondary");
    }
    btn.getStyleClass().remove("btn-secondary");
    btn.getStyleClass().add("btn-primary");
    activeFilter = btn;
  }

  // ── Tab navigation ────────────────────────────────────────

  /** Switches to the Overview tab. */
  @FXML
  public void onTabOverview() {
    showTab(overviewTab, navOverview);
  }

  /** Switches to the Deposit tab. */
  @FXML
  public void onTabDeposit() {
    showTab(depositTab, navDeposit);
  }

  /** Switches to the Withdraw tab. */
  @FXML
  public void onTabWithdraw() {
    showTab(withdrawTab, navWithdraw);
  }

  /** Switches to the History tab. */
  @FXML
  public void onTabHistory() {
    showTab(historyTab, navHistory);
  }

  /**
   * Shows one tab and hides the others.
   *
   * @param tab the VBox tab to show
   * @param btn the nav button to activate
   */
  private void showTab(final VBox tab, final Button btn) {
    overviewTab.setVisible(false);
    overviewTab.setManaged(false);
    depositTab.setVisible(false);
    depositTab.setManaged(false);
    withdrawTab.setVisible(false);
    withdrawTab.setManaged(false);
    historyTab.setVisible(false);
    historyTab.setManaged(false);
    tab.setVisible(true);
    tab.setManaged(true);
    if (activeNav != null) {
      activeNav.getStyleClass().remove("nav-item-active");
      activeNav.getStyleClass().add("nav-item");
    }
    btn.getStyleClass().remove("nav-item");
    btn.getStyleClass().add("nav-item-active");
    activeNav = btn;
  }

  // ── Transaction row builder ───────────────────────────────

  /**
   * Delegates row building to TransactionUIHelper.
   *
   * @param tx     transaction data array [desc, amt, date, type]
   * @param isLast whether this is the last row (no bottom border)
   * @return the assembled HBox node
   */
  private HBox buildRow(final String[] tx, final boolean isLast) {
    return TransactionUiHelper.buildTxRow(tx[0], tx[1], tx[2], tx[3], isLast);
  }

  // ── Screen navigation ─────────────────────────────────────

  /** Toggles dark/light mode. */
  @FXML
  private void onToggleTheme() {
    SceneNavigator.toggleTheme();
  }

  /** Navigates to the home screen. */
  @FXML
  private void onHome() {
    SceneNavigator.navigateTo(SceneNavigator.View.HOME);
  }

  /** Navigates to the my-bids screen. */
  @FXML
  private void onMyBids() {
    SceneNavigator.navigateTo(SceneNavigator.View.MY_BIDS);
  }

  /** Navigates to the profile screen. */
  @FXML
  private void onProfile() {
    SceneNavigator.navigateTo(SceneNavigator.View.PROFILE);
  }

  /** Logs out and navigates to the login screen. */
  @FXML
  private void onLogout() {
    UserSession.getInstance().clear();
    SceneNavigator.navigateTo(SceneNavigator.View.LOGIN);
  }

  /**
   * Formats a monetary value as a USD string.
   *
   * @param amount the amount in USD
   * @return formatted string, e.g. "$1,240.00"
   */
  private String formatMoney(final double amount) {
    return String.format("$%,.2f", amount);
  }
}
