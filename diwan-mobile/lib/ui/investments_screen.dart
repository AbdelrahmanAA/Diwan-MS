import 'package:flutter/material.dart';

import '../l10n/app_localizations.dart';
import 'alerts_tab.dart';
import 'news_tab.dart';
import 'portfolios_tab.dart';
import 'watchlist_tab.dart';

/// The investments section (opened from the dashboard): portfolios, watchlist, news and alerts as tabs.
class InvestmentsScreen extends StatefulWidget {
  const InvestmentsScreen({super.key});

  @override
  State<InvestmentsScreen> createState() => _InvestmentsScreenState();
}

class _InvestmentsScreenState extends State<InvestmentsScreen> {
  int _tab = 0;

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    const pages = [PortfoliosTab(), WatchlistTab(), NewsTab(), AlertsTab()];
    return Scaffold(
      body: IndexedStack(index: _tab, children: pages),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _tab,
        onDestinationSelected: (i) => setState(() => _tab = i),
        destinations: [
          NavigationDestination(icon: const Icon(Icons.pie_chart_outline), selectedIcon: const Icon(Icons.pie_chart), label: t.portfolios),
          NavigationDestination(icon: const Icon(Icons.visibility_outlined), selectedIcon: const Icon(Icons.visibility), label: t.watchlist),
          NavigationDestination(icon: const Icon(Icons.newspaper_outlined), selectedIcon: const Icon(Icons.newspaper), label: t.news),
          NavigationDestination(icon: const Icon(Icons.notifications_outlined), selectedIcon: const Icon(Icons.notifications), label: t.alerts),
        ],
      ),
    );
  }
}
