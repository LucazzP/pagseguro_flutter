package com.example.pagseguro
import android.os.Bundle
import android.text.TextUtils
import br.com.uol.pagseguro.plugpag.*
import io.flutter.app.FlutterActivity
import io.flutter.plugin.common.MethodChannel
import br.com.uol.pagseguro.plugpag.PlugPagTransactionResult
import br.com.uol.pagseguro.plugpag.PlugPagVoidData
import br.com.uol.pagseguro.plugpag.PlugPagPaymentData
import br.com.uol.pagseguro.plugpag.PlugPag
import com.example.pagseguro.task.PinpadVoidPaymentTask
import com.example.pagseguro.task.PinpadPaymentTask
import com.example.pagseguro.task.TerminalQueryTransactionTask
import com.example.pagseguro.task.TerminalVoidPaymentTask
import com.example.pagseguro.task.TerminalPaymentTask
import com.example.pagseguro.helper.Generator
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import android.content.pm.PackageInfo
import android.support.v4.app.ActivityCompat
import com.example.pagseguro.helper.Bluetooth
import android.support.v7.app.AlertDialog




class MainActivity : FlutterActivity(), TaskHandler, PlugPagAuthenticationListener {

  private val PERMISSIONS_REQUEST_CODE = 0x1234
  private val CHANNEL = "pagseguro"
  private var mAlertDialog: AlertDialog? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    PlugPagManager.create(this.applicationContext)
    MethodChannel(flutterView, CHANNEL).setMethodCallHandler { call, result ->

      when (call.method) {
        //PERMISSIONS
        "requestPermission" ->  this.requestPermissions()

        //Auth
        "requestAuthentication" -> this.requestAuthentication()
        "checkAuthentication" -> this.checkAuthentication()
        "invalidateAuthentication" -> this.invalidateAuthentication()

        //Terminal
        "startTerminalCreditPayment" -> this.startTerminalCreditPayment(call.arguments as Int)
        "startTerminalCreditWithInstallmentsPayment" -> this.startTerminalCreditWithInstallmentsPayment((call.arguments as List<*>)[0] as Int,(call.arguments as List<*>)[1] as Int)
        "startTerminalDebitPayment" -> this.startTerminalDebitPayment(call.arguments as Int)
        "startTerminalVoucherPayment" -> this.startTerminalVoucherPayment(call.arguments as Int)
        "startTerminalVoidPayment" -> this.startTerminalVoidPayment()
        "startTerminalQueryTransaction" -> this.startTerminalQueryTransaction()

        //Pinpad
        "startPinpadCreditPayment" -> this.startPinpadCreditPayment(call.arguments as Int)
        "startPinpadCreditWithInstallmentsPayment" -> this.startPinpadCreditWithInstallmentsPayment((call.arguments as List<*>)[0] as Int,(call.arguments as List<*>)[1] as Int)
        "startPinpadDebitPayment" -> this.startPinpadDebitPayment(call.arguments as Int)
        "startPinpadVoucherPayment" -> this.startPinpadVoucherPayment(call.arguments as Int)
        "startPinpadVoidPayment" -> this.startPinpadVoidPayment()

        else -> result.notImplemented()
      }



    }
  }


  // -----------------------------------------------------------------------------------------------------------------
  // Request missing permissions
  // -----------------------------------------------------------------------------------------------------------------

  /**
   * Requests permissions on runtime, if any needed permission is not granted.
   */
  private fun requestPermissions() {
    var missingPermissions: Array<String>? = null

    missingPermissions = this.filterMissingPermissions(this.getManifestPermissions())

    if (missingPermissions != null && missingPermissions.size > 0) {
      ActivityCompat.requestPermissions(this, missingPermissions, PERMISSIONS_REQUEST_CODE)
    } else {
      this.showMessage(R.string.msg_all_permissions_granted)
    }
  }

  /**
   * Returns a list of permissions requested on the AndroidManifest.xml file.
   *
   * @return Permissions requested on the AndroidManifest.xml file.
   */
  private fun getManifestPermissions(): Array<String> {
    var permissions: Array<String>? = null
    var info: PackageInfo? = null

    try {
      info = this.packageManager
              .getPackageInfo(this.applicationContext.packageName, PackageManager.GET_PERMISSIONS)
      permissions = info!!.requestedPermissions
    } catch (e: PackageManager.NameNotFoundException) {

    }

    if (permissions == null) {
      permissions = arrayOf()
    }

    return permissions
  }

  /**
   * Filters only the permissions still not granted.
   *
   * @param permissions List of permissions to be checked.
   * @return Permissions not granted.
   */
  private fun filterMissingPermissions(permissions: Array<String>?): Array<String>? {
    var missingPermissions: Array<String>? = null
    var list: MutableList<String>? = null

    list = ArrayList()

    if (permissions != null && permissions.size > 0) {
      for (permission in permissions) {
        if (ContextCompat.checkSelfPermission(this.applicationContext, permission) != PackageManager.PERMISSION_GRANTED) {
          list.add(permission)
        }
      }
    }

    missingPermissions = list.toTypedArray()

    return missingPermissions
  }

  // -----------------------------------------------------------------------------------------------------------------
  // Authentication handling
  // -----------------------------------------------------------------------------------------------------------------

  /**
   * Checks if a user is authenticated.
   */
  private fun checkAuthentication(){
    if (PlugPagManager.getInstance().plugPag.isAuthenticated) {
      this.showMessage(R.string.msg_authentication_ok)
    } else {
      this.showMessage(R.string.msg_authentication_missing)
    }
  }

  /**
   * Requests authentication.
   */
  private fun requestAuthentication() {
    PlugPagManager.getInstance().plugPag.requestAuthentication(this)
  }

  /**
   * Invalidates current authentication.
   */
  private fun invalidateAuthentication() {
    PlugPagManager.getInstance().plugPag.invalidateAuthentication()
  }

  // -----------------------------------------------------------------------------------------------------------------
  // Terminal transactions
  // -----------------------------------------------------------------------------------------------------------------

  /**
   * Starts a new credit payment on a terminal.
   */
  private fun startTerminalCreditPayment(amount: Int) {
    var paymentData: PlugPagPaymentData? = null

    paymentData = PlugPagPaymentData.Builder()
            .setType(PlugPag.TYPE_CREDITO)
            .setInstallmentType(PlugPag.INSTALLMENT_TYPE_A_VISTA)
            .setAmount(amount)
            .setUserReference(this.getString(R.string.plugpag_user_reference))
            .build()

    TerminalPaymentTask(this).execute(paymentData)
  }

  /**
   * Starts a new credit payment with installments on a terminal
   */
  private fun startTerminalCreditWithInstallmentsPayment(amount: Int, installments: Int) {
    var paymentData: PlugPagPaymentData? = null

    paymentData = PlugPagPaymentData.Builder()
            .setType(PlugPag.TYPE_CREDITO)
            .setInstallmentType(PlugPag.INSTALLMENT_TYPE_PARC_VENDEDOR)
            .setInstallments(installments)
            .setAmount(amount)
            .setUserReference(this.getString(R.string.plugpag_user_reference))
            .build()
    TerminalPaymentTask(this).execute(paymentData)
  }

  /**
   * Starts a new debit payment on a terminal.
   */
  private fun startTerminalDebitPayment(amount: Int) {
    var paymentData: PlugPagPaymentData? = null

    paymentData = PlugPagPaymentData.Builder()
            .setType(PlugPag.TYPE_DEBITO)
            .setAmount(amount)
            .setUserReference(this.getString(R.string.plugpag_user_reference))
            .build()
    TerminalPaymentTask(this).execute(paymentData)
  }

  /**
   * Starts a new voucher payment on a terminal.
   */
  private fun startTerminalVoucherPayment(amount: Int) {
    var paymentData: PlugPagPaymentData? = null

    paymentData = PlugPagPaymentData.Builder()
            .setType(PlugPag.TYPE_VOUCHER)
            .setAmount(amount)
            .setUserReference(this.getString(R.string.plugpag_user_reference))
            .build()
    TerminalPaymentTask(this).execute(paymentData)
  }

  /**
   * Starts a new void payment on a terminal.
   */
  private fun startTerminalVoidPayment() {
    TerminalVoidPaymentTask(this).execute()
  }

  /**
   * Starts a new transaction query on a terminal.
   */
  private fun startTerminalQueryTransaction() {
    TerminalQueryTransactionTask(this).execute()
  }

  // -----------------------------------------------------------------------------------------------------------------
  // Pinpad transactions
  // -----------------------------------------------------------------------------------------------------------------

  /**
   * Starts a new credit payment on a pinpad.
   */
  private fun startPinpadCreditPayment(amount: Int) {
    var paymentData: PlugPagPaymentData? = null

    paymentData = PlugPagPaymentData.Builder()
            .setType(PlugPag.TYPE_CREDITO)
            .setInstallmentType(PlugPag.INSTALLMENT_TYPE_A_VISTA)
            .setAmount(amount)
            .setUserReference(this.getString(R.string.plugpag_user_reference))
            .build()
    PinpadPaymentTask(this).execute(paymentData)
  }

  /**
   * Starts a new credit payment with installments on a pinpad.
   */
  private fun startPinpadCreditWithInstallmentsPayment(amount: Int,installments: Int) {
    var paymentData: PlugPagPaymentData? = null

    paymentData = PlugPagPaymentData.Builder()
            .setType(PlugPag.TYPE_CREDITO)
            .setInstallmentType(PlugPag.INSTALLMENT_TYPE_PARC_VENDEDOR)
            .setInstallments(installments)
            .setAmount(amount)
            .setUserReference(this.getString(R.string.plugpag_user_reference))
            .build()
    PinpadPaymentTask(this).execute(paymentData)
  }

  /**
   * Starts a new debit payment on a pinpad.
   */
  private fun startPinpadDebitPayment(amount: Int) {
    var paymentData: PlugPagPaymentData? = null

    paymentData = PlugPagPaymentData.Builder()
            .setType(PlugPag.TYPE_DEBITO)
            .setAmount(amount)
            .setUserReference(this.getString(R.string.plugpag_user_reference))
            .build()
    PinpadPaymentTask(this).execute(paymentData)
  }

  /**
   * Starts a new voucher payment on a pinpad.
   */
  private fun startPinpadVoucherPayment(amount: Int) {
    var paymentData: PlugPagPaymentData? = null

    paymentData = PlugPagPaymentData.Builder()
            .setType(PlugPag.TYPE_VOUCHER)
            .setAmount(amount)
            .setUserReference(this.getString(R.string.plugpag_user_reference))
            .build()
    PinpadPaymentTask(this).execute(paymentData)
  }

  /**
   * Starts a void payment on a pinpad.
   */
  private fun startPinpadVoidPayment() {
    var voidData: PlugPagVoidData? = null
    var lastTransaction: Array<String>? = null

    lastTransaction = PreviousTransactions.pop()

    if (lastTransaction != null) {
      voidData = PlugPagVoidData.Builder()
              .setTransactionCode(lastTransaction[0])
              .setTransactionId(lastTransaction[1])
              .build()
      PinpadVoidPaymentTask(this).execute(voidData)
    } else {
      this.showErrorMessage(R.string.msg_error_missing_transaction_data)
    }
  }

  // -----------------------------------------------------------------------------------------------------------------
  // AlertDialog
  // -----------------------------------------------------------------------------------------------------------------

  /**
   * Shows an AlertDialog with a simple message.
   *
   * @param message Message to be displayed.
   */
  private fun showMessage(message: String?) {

    if (TextUtils.isEmpty(message)) {
        MethodChannel(flutterView, CHANNEL).invokeMethod("showMessage",R.string.msg_error_unexpected)
    } else {
        MethodChannel(flutterView, CHANNEL).invokeMethod("showMessage",message)
    }
  }

  /**
   * Shows an AlertDialog with a simple message.
   *
   * @param message Resource ID of the message to be displayed.
   */
  private fun showMessage( message: Int) {
    var msg: String? = null

    msg = this.getString(message)
    this.showMessage(msg)
  }

  /**
   * Shows an AlertDialog with an error message.
   *
   * @param message Message to be displayed.
   */
  private fun showErrorMessage(message: String) {

      if (TextUtils.isEmpty(message)) {
          MethodChannel(flutterView, CHANNEL).invokeMethod("showErrorMessage",R.string.msg_error_unexpected)
      } else {
          MethodChannel(flutterView, CHANNEL).invokeMethod("showErrorMessage",R.string.title_error)
      }

  }

  /**
   * Shows an AlertDialog with an error message.
   *
   * @param message Resource ID of the message to be displayed.
   */
  private fun showErrorMessage( message: Int) {
    var msg: String? = null

    msg = this.getString(message)
    this.showErrorMessage(msg)
  }

  /**
   * Shows an AlertDialog with a ProgressBar.
   *
   * @param message Message to be displayed along-side with the ProgressBar.
   */
  private fun showProgressDialog( message: String?) {
    var msg: String? = null

    if (message == null) {
      msg = this.getString(R.string.msg_wait)
    } else {
      msg = message
    }
      if (TextUtils.isEmpty(msg)) {
          MethodChannel(flutterView, CHANNEL).invokeMethod("showProgressDialog",msg)
      } else {
          MethodChannel(flutterView, CHANNEL).invokeMethod("showProgressDialog",msg)
      }

  }

  /**
   * Shows an AlertDialog with a ProgressBar.
   *
   * @param message Resource ID of the message to be displayed along-side with the ProgressBar.
   */
  private fun showProgressDialog(message: Int) {
    var msg: String? = null

    msg = this.getString(message)
    this.showProgressDialog(msg)
  }

  // -----------------------------------------------------------------------------------------------------------------
  // Task handling
  // -----------------------------------------------------------------------------------------------------------------

  override fun onTaskStart() {
    this.showProgressDialog(R.string.msg_wait)
  }

  override fun onProgressPublished(progress: String, transactionInfo: Any) {
    var message: String? = null
    var type: String? = null

    if (TextUtils.isEmpty(progress)) {
      message = this.getString(R.string.msg_wait)
    } else {
      message = progress
    }

    if (transactionInfo is PlugPagPaymentData) {
      when (transactionInfo.type) {
        PlugPag.TYPE_CREDITO -> type = this.getString(R.string.type_credit)

        PlugPag.TYPE_DEBITO -> type = this.getString(R.string.type_debit)

        PlugPag.TYPE_VOUCHER -> type = this.getString(R.string.type_voucher)
      }

      message = this.getString(
              R.string.msg_payment_info,
              type,
              transactionInfo.amount.toDouble() / 100.0,
              transactionInfo.installments,
              message)
    } else if (transactionInfo is PlugPagVoidData) {
      message = this.getString(R.string.msg_void_payment_info, message)
    }

    this.showProgressDialog(message)
  }

  override fun onTaskFinished(result: Any) {
    if (result is PlugPagTransactionResult) {
      this.showResult(result)
    } else if (result is String) {
      this.showMessage(result)
    }
  }

  // -----------------------------------------------------------------------------------------------------------------
  // Result display
  // -----------------------------------------------------------------------------------------------------------------

  /**
   * Shows a transaction's result.
   *
   * @param result Result to be displayed.
   */
  private fun showResult(result: PlugPagTransactionResult) {
    var resultDisplay: String? = null
    var lines: MutableList<String>? = null

    if (result == null) {
      throw RuntimeException("Transaction result cannot be null")
    }

    lines = ArrayList()
    lines.add(this.getString(R.string.msg_result_result, result.result))

    if (!TextUtils.isEmpty(result.errorCode)) {
      lines.add(this.getString(R.string.msg_result_error_code, result.errorCode))
    }

    if (!TextUtils.isEmpty(result.amount)) {
      var value: String? = null

      value = String.format("%.2f",
              java.lang.Double.parseDouble(result.amount) / 100.0)
      lines.add(this.getString(R.string.msg_result_amount, value))
    }

    if (!TextUtils.isEmpty(result.availableBalance)) {
      var value: String? = null

      value = String.format("%.2f",
              java.lang.Double.parseDouble(result.amount) / 100.0)
      lines.add(this.getString(R.string.msg_result_available_balance, value))
    }

    if (!TextUtils.isEmpty(result.bin)) {
      lines.add(this.getString(R.string.msg_result_bin, result.bin))
    }

    if (!TextUtils.isEmpty(result.cardBrand)) {
      lines.add(this.getString(R.string.msg_result_card_brand, result.cardBrand))
    }

    if (!TextUtils.isEmpty(result.date)) {
      lines.add(this.getString(R.string.msg_result_date, result.date))
    }

    if (!TextUtils.isEmpty(result.time)) {
      lines.add(this.getString(R.string.msg_result_time, result.time))
    }

    if (!TextUtils.isEmpty(result.holder)) {
      lines.add(this.getString(R.string.msg_result_holder, result.holder))
    }

    if (!TextUtils.isEmpty(result.hostNsu)) {
      lines.add(this.getString(R.string.msg_result_host_nsu, result.hostNsu))
    }

    if (!TextUtils.isEmpty(result.message)) {
      lines.add(this.getString(R.string.msg_result_message, result.message))
    }

    if (!TextUtils.isEmpty(result.terminalSerialNumber)) {
      lines.add(this.getString(R.string.msg_result_serial_number, result.terminalSerialNumber))
    }

    if (!TextUtils.isEmpty(result.transactionCode)) {
      lines.add(this.getString(R.string.msg_result_transaction_code, result.transactionCode))
    }

    if (!TextUtils.isEmpty(result.transactionId)) {
      lines.add(this.getString(R.string.msg_result_transaction_id, result.transactionId))
    }

    if (!TextUtils.isEmpty(result.userReference)) {
      lines.add(this.getString(R.string.msg_result_user_reference, result.userReference))
    }

    resultDisplay = TextUtils.join("\n", lines)
    this.showMessage(resultDisplay)
  }

  override fun onSuccess() {
    this.showMessage(R.string.msg_authentication_ok)
  }

  override fun onError() {
    this.showMessage(R.string.msg_authentication_failed)
  }
}
