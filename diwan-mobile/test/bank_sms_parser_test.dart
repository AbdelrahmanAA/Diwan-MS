// Al Rajhi Bank SMS samples (provided by the user, Oct 2026) and what the app stores for each.
import 'package:diwan_mobile/sms/bank_sms_parser.dart';
import 'package:flutter_test/flutter_test.dart';

const _sender = 'AlRajhiBank';
final _parser = BankSmsParser();

/// Parses [body] as if it arrived at [received] ("yyyy-MM-dd HH:mm").
SmsParseResult _parse(String body, String received) => _parser.parse(_sender, body, DateTime.parse(received));

SmsTransaction _tx(String body, String received) {
  final r = _parse(body, received);
  expect(r.outcome, SmsOutcome.transaction, reason: 'reason: ${r.reason}');
  return r.transaction!;
}

void _expectTx(
  SmsTransaction t, {
  required String type,
  required String category,
  required String amount,
  String currency = 'SAR',
  required String at,
  String? merchant,
  String? payment,
}) {
  expect(t.type, type);
  expect(t.category, category);
  expect(t.amount, amount);
  expect(t.currency, currency);
  expect(t.recordedAt, DateTime.parse(at));
  expect(t.merchantName, merchant);
  if (payment != null) expect(t.paymentMethod, payment);
}

void main() {
  group('own money moving (transfer, not income/expense)', () {
    test('transfer between your accounts', () {
      final t = _tx('Transfer Between Your Accounts\nAmount:SR 5500\nFrom:2410\nTo:8468\n26/10/25 22:26', '2026-10-25 22:26');
      _expectTx(t, type: 'transfer', category: 'transfer', amount: '5500', at: '2026-10-25 22:26');
      expect(t.notes, 'From: 2410 · To: 8468');
    });

    test('credit card payment', () {
      final t = _tx('Credit Card:Payment\nCard:Visa 1553\nAmount:SR 4980\nBalance:5844.89 SR\n25/10/26 22:25', '2026-10-25 22:25');
      _expectTx(t, type: 'transfer', category: 'transfer', amount: '4980', at: '2026-10-25 22:25');
      expect(t.notes, 'Card: Visa 1553 · Balance: 5844.89 SR');
      expect(t.description, 'Credit Card:Payment');
    });
  });

  group('spending', () {
    test('bill payment keeps the biller and the bill number', () {
      final t = _tx('Bill Payment\nFrom:2410\nAmount:SR 2572.33\nBiller:125\nService:ENBD PAYMENTS\nBill:0196074079101\n26/10/25 22:07',
          '2026-10-25 22:07');
      _expectTx(t, type: 'expense', category: 'bills', amount: '2572.33', at: '2026-10-25 22:07', merchant: 'ENBD PAYMENTS');
      expect(t.referenceNumber, '0196074079101');
      expect(t.notes, 'From: 2410 · Biller: 125');
    });

    test('transfer to another person: the name, not the account, is the merchant', () {
      final t = _tx('Debit Internal Transfer\nFrom:2410\nAmount:SR 5000\nTo:MOHAMED HASSAN\nTo:2368\n26/10/25 21:58', '2026-10-25 21:58');
      _expectTx(t, type: 'expense', category: 'transfer', amount: '5000', at: '2026-10-25 21:58', merchant: 'MOHAMED HASSAN');
      expect(t.notes, 'From: 2410 · To: 2368');
    });

    test('PoS purchase with Samsung Pay is a wallet payment', () {
      final t = _tx('PoS purchase\nCard:1553 ;Visa-Samsung Pay\nAt: TASALIMAR\nAmount:103.51 SAR\nBalance: 864.89 SAR\n25/10/26 21:18',
          '2026-10-25 21:18');
      _expectTx(t,
          type: 'expense', category: 'shopping', amount: '103.51', at: '2026-10-25 21:18', merchant: 'TASALIMAR', payment: 'wallet');
      expect(t.notes, 'Card: 1553 Visa-Samsung Pay · Balance: 864.89 SAR');
    });

    test('loan instalment', () {
      final t = _tx('Debit: Loan Instalment\nInstalment: SAR 5210\nFrom: 2410\nRemaining Amount: SAR 41680\n25/10/26 20:05',
          '2026-10-25 20:05');
      _expectTx(t, type: 'expense', category: 'loan', amount: '5210', at: '2026-10-25 20:05');
      expect(t.notes, 'From: 2410 · Remaining Amount: SAR 41680');
    });

    test('online purchase with mada', () {
      final t = _tx('Online Purchase\nBy:5101;mada\nFrom:2410\nAmount:SR 5000\nAt:barq\n25/10/26 16:10', '2026-10-25 16:10');
      _expectTx(t, type: 'expense', category: 'shopping', amount: '5000', at: '2026-10-25 16:10', merchant: 'barq', payment: 'card');
    });

    test('ATM withdrawal', () {
      final t = _tx('Withdrawal:ATM\nBy:5101;mada\nAmount:SR 3000\nPlace:ALRAJHI BANK OPR BLDG 1\n28/9/26 11:44', '2026-09-28 11:44');
      _expectTx(t,
          type: 'expense', category: 'cash', amount: '3000', at: '2026-09-28 11:44', merchant: 'ALRAJHI BANK OPR BLDG 1', payment: 'cash');
    });

    test('foreign purchase is stored with the SAR total due (fees included), the USD amount goes to the notes', () {
      final t = _tx(
          'Online Purchase\nCard:1500 ;Visa\nAmount:57USD(214.05 SAR)\nAt: CCA*VF Wo\nFee &VAT: 4.92 SAR\nExchange rate~ 3.755263\n'
          'Total due amount:218.97 SAR\nCountry:UAE\nBalance:955.49 SAR\n25/8/26 22:07',
          '2026-08-25 22:07');
      _expectTx(t, type: 'expense', category: 'shopping', amount: '218.97', at: '2026-08-25 22:07', merchant: 'CCA*VF Wo');
      expect(t.notes, contains('Amount: 57USD(214.05 SAR)'));
      expect(t.notes, contains('Fee &VAT: 4.92 SAR'));
      expect(t.notes, contains('Exchange rate: 3.755263'));
      expect(t.notes, contains('Country: UAE'));
    });

    test('foreign purchase without a SAR amount stays in its currency', () {
      final t = _tx('Online Purchase\nBy:1553 ;Visa\nAmount:8.75 USD\nAt:MOFA-VISA\nBalance:184.41 SR\n14/10/26 10:44', '2026-10-14 10:44');
      _expectTx(t, type: 'expense', category: 'shopping', amount: '8.75', currency: 'USD', at: '2026-10-14 10:44', merchant: 'MOFA-VISA');
    });
  });

  group('money in', () {
    test('transfer from another person', () {
      final t = _tx('Credit Transfer Internal\nAmount:SR 471.50\nTo:2410\nFrom:KHALED MOUS\nFrom:7496\n26/10/25 21:54', '2026-10-25 21:54');
      _expectTx(t, type: 'income', category: 'transfer', amount: '471.50', at: '2026-10-25 21:54', merchant: 'KHALED MOUS');
    });

    test('salary', () {
      final t = _tx('Credit transfer Salary\nAmount:SAR 34965\nTo:2410\n25/10/26 00:23', '2026-10-25 00:23');
      _expectTx(t, type: 'income', category: 'salary', amount: '34965', at: '2026-10-25 00:23');
    });

    test('credit card refund (date line with "Date:")', () {
      final t = _tx('Credit Card Refund\nCard: 1553; Visa\nAmount: 37.3 SAR\nFrom: TRENDYOL A \nDate: 17/10/26 10:42', '2026-10-17 10:42');
      _expectTx(t, type: 'income', category: 'refund', amount: '37.3', at: '2026-10-17 10:42', merchant: 'TRENDYOL A');
    });

    test('reversed transaction', () {
      final t = _tx('Reverse Transaction\nCard:1553;Visa\nAmount:SAR 9000\nBalance:SAR 9999.53\n14/9/26 21:20', '2026-09-14 21:20');
      _expectTx(t, type: 'income', category: 'refund', amount: '9000', at: '2026-09-14 21:20');
    });

    test('savings profit', () {
      final t = _tx('Deposit:Saving Account Monthly Profit\nAmount:SAR 7.62\nTo:8468\n1/10/26 06:48', '2026-10-01 06:48');
      _expectTx(t, type: 'income', category: 'profit', amount: '7.62', at: '2026-10-01 06:48');
    });

    test('ATM deposit', () {
      final t = _tx('Deposit:ATM\nAmount:SAR 41000\nTo:2410\n10/6/26 22:02', '2026-06-10 22:02');
      _expectTx(t, type: 'income', category: 'cash', amount: '41000', at: '2026-06-10 22:02', payment: 'cash');
    });
  });

  group('never stored', () {
    test('declined transaction', () {
      final r = _parse('Transaction Declined: Insufficient funds.\nTransaction: PoS\nCard: 1553\nAmount: SAR 90.08\nAt: DANUBE-AL\n'
          'Date: 24/10/26 19:36', '2026-10-24 19:36');
      expect(r.outcome, SmsOutcome.dropped);
      expect(r.reason, 'declined');
    });

    test('OTP codes, also with an amount', () {
      for (final body in [
        'OTP Code:7549\nReason:Beneficiary Addition - Mobile App',
        'OTP Code:6379\nReason:Rajhi Transfer - Mobile App\nAmount:5,000.00 SAR',
      ]) {
        final r = _parse(body, '2026-10-25 12:00');
        expect(r.outcome, SmsOutcome.dropped);
        expect(r.reason, 'otp');
        expect(BankSmsParser.isOtp(body), isTrue);
      }
    });

    test('unknown bank message', () {
      expect(_parse('Dear customer, our branches are closed on Friday.', '2026-10-25 12:00').outcome, SmsOutcome.unknown);
    });
  });

  group('dates', () {
    test('the reading closest to the arrival time wins (yy/mm/dd and d/m/yy both occur)', () {
      const body = 'Transfer Between Your Accounts\nAmount:SR 5500\nFrom:2410\nTo:8468\n26/10/25 22:26';
      expect(_tx(body, '2026-10-25 22:30').recordedAt, DateTime.parse('2026-10-25 22:26'));
      // imported from the inbox the next morning: still the time of the message, not of the import
      expect(_tx(body, '2026-10-26 08:00').recordedAt, DateTime.parse('2026-10-25 22:26'));
    });

    test('a date far from the arrival time is not trusted', () {
      const body = 'Deposit:ATM\nAmount:SAR 100\nTo:2410\n10/6/26 22:02';
      expect(_tx(body, '2026-10-01 09:00').recordedAt, DateTime.parse('2026-10-01 09:00'));
    });
  });

  test('json sent to the server', () {
    final j = _tx('PoS purchase\nCard:1553 ;Visa-Samsung Pay\nAt: TASALIMAR\nAmount:103.51 SAR\n25/10/26 21:18', '2026-10-25 21:18').toJson();
    expect(j['bank'], 'Al Rajhi');
    expect(j['tags'], 'sms');
    expect(j['status'], 'completed');
    expect(j['recordedAt'], '2026-10-25T21:18:00');
    expect(j['sourceRef'], hasLength(64));
    expect(j.containsKey('body'), isFalse); // the SMS text never leaves the phone
  });

  test('fingerprint is the same for the receiver and the inbox copy of a message', () {
    final a = BankSmsParser.fingerprint('AlRajhiBank', 'Deposit:ATM\r\nAmount:SAR 41000 \r\nTo:2410\r\n10/6/26 22:02\r\n');
    final b = BankSmsParser.fingerprint('ALRAJHIBANK', 'Deposit:ATM\nAmount:SAR 41000\nTo:2410\n10/6/26 22:02');
    expect(a, b);
    expect(a, isNot(BankSmsParser.fingerprint('AlRajhiBank', 'Deposit:ATM\nAmount:SAR 41001\nTo:2410\n10/6/26 22:02')));
  });

  test('senders', () {
    expect(BankSmsParser.isBankSender('AlRajhiBank'), isTrue);
    expect(BankSmsParser.isBankSender('Al-Rajhi'), isTrue);
    expect(BankSmsParser.isBankSender('STC'), isFalse);
    expect(BankSmsParser.isBankSender('+966500000000'), isFalse);
  });

  group('positions: own account, card and balance of each message', () {
    // [body, account, counter account, card, network, balance, balance of]
    final cases = <List<String?>>[
      ['Transfer Between Your Accounts\nAmount:SR 5500\nFrom:2410\nTo:8468\n26/10/25 22:26', '2410', '8468', null, null, null, null],
      ['Credit Card:Payment\nCard:Visa 1553\nAmount:SR 4980\nBalance:5844.89 SR\n25/10/26 22:25', null, null, '1553', 'Visa', '5844.89', 'CARD'],
      ['Bill Payment\nFrom:2410\nAmount:SR 2572.33\nBiller:125\nService:ENBD PAYMENTS\nBill:0196074079101\n26/10/25 22:07', '2410', null, null, null, null, null],
      // To:2368 is the other person's account, not one of the user's
      ['Debit Internal Transfer\nFrom:2410\nAmount:SR 5000\nTo:MOHAMED HASSAN\nTo:2368\n26/10/25 21:58', '2410', null, null, null, null, null],
      ['Credit Transfer Internal\nAmount:SR 471.50\nTo:2410\nFrom:KHALED MOUS\nFrom:7496\n26/10/25 21:54', '2410', null, null, null, null, null],
      ['PoS purchase\nCard:1553 ;Visa-Samsung Pay\nAt: TASALIMAR\nAmount:103.51 SAR\nBalance: 864.89 SAR\n25/10/26 21:18', null, null, '1553', 'Visa', '864.89', 'CARD'],
      ['Debit: Loan Instalment\nInstalment: SAR 5210\nFrom: 2410\nRemaining Amount: SAR 41680\n25/10/26 20:05', '2410', null, null, null, '41680', 'LOAN'],
      ['Online Purchase\nBy:5101;mada\nFrom:2410\nAmount:SR 5000\nAt:barq\n25/10/26 16:10', '2410', null, '5101', 'mada', null, null],
      ['Withdrawal:ATM\nBy:5101;mada\nAmount:SR 3000\nPlace:ALRAJHI BANK OPR BLDG 1\n28/9/26 11:44', null, null, '5101', 'mada', null, null],
      ['Credit Card Refund\nCard: 1553; Visa\nAmount: 37.3 SAR\nFrom: TRENDYOL A \nDate: 17/10/26 10:42', null, null, '1553', 'Visa', null, null],
      ['Reverse Transaction\nCard:1553;Visa\nAmount:SAR 9000\nBalance:SAR 9999.53\n14/9/26 21:20', null, null, '1553', 'Visa', '9999.53', 'CARD'],
      ['Credit transfer Salary\nAmount:SAR 34965\nTo:2410\n25/10/26 00:23', '2410', null, null, null, null, null],
      ['Online Purchase\nCard:1500 ;Visa\nAmount:57USD(214.05 SAR)\nAt: CCA*VF Wo\nFee &VAT: 4.92 SAR\nExchange rate~ 3.755263\n'
          'Total due amount:218.97 SAR\nCountry:UAE\nBalance:955.49 SAR\n25/8/26 22:07', null, null, '1500', 'Visa', '955.49', 'CARD'],
      ['Online Purchase\nBy:1553 ;Visa\nAmount:8.75 USD\nAt:MOFA-VISA\nBalance:184.41 SR\n14/10/26 10:44', null, null, '1553', 'Visa', '184.41', 'CARD'],
      ['Deposit:Saving Account Monthly Profit\nAmount:SAR 7.62\nTo:8468\n1/10/26 06:48', '8468', null, null, null, null, null],
      ['Deposit:ATM\nAmount:SAR 41000\nTo:2410\n10/6/26 22:02', '2410', null, null, null, null, null],
    ];
    for (final c in cases) {
      test(c[0]!.split('\n').first, () {
        final t = _parser.parse(_sender, c[0]!, DateTime(2026, 10, 25)).transaction!;
        expect([t.accountRef, t.counterAccountRef, t.cardRef, t.cardNetwork, t.balance, t.balanceOf], c.sublist(1));
        if (t.balance != null) expect(t.balanceCurrency, 'SAR');
      });
    }

    test('sent to the server', () {
      final j = _tx('PoS purchase\nCard:1553 ;Visa-Samsung Pay\nAt: TASALIMAR\nAmount:103.51 SAR\nBalance: 864.89 SAR\n25/10/26 21:18',
              '2026-10-25 21:18')
          .toJson();
      expect([j['cardRef'], j['cardNetwork'], j['balance'], j['balanceCurrency'], j['balanceOf']],
          ['1553', 'Visa', '864.89', 'SAR', 'CARD']);
    });
  });
}
