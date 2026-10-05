import 'package:flutter/material.dart';

import '../l10n/app_localizations.dart';
import 'alerts_tab.dart';
import 'news_tab.dart';
import 'portfolios_tab.dart';
import 'settings_tab.dart';
import 'watchlist_tab.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _tab = 0;

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    const pages = [PortfoliosTab(), WatchlistTab(), NewsTab(), AlertsTab(), SettingsTab()];
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
          NavigationDestination(icon: const Icon(Icons.settings_outlined), selectedIcon: const Icon(Icons.settings), label: t.settings),
        ],
      ),
    );
  }
}
