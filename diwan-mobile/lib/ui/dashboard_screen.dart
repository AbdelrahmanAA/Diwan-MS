import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/models.dart';
import '../l10n/app_localizations.dart';
import '../state/session.dart';
import 'investments_screen.dart';
import 'medical_screen.dart';
import 'money_screen.dart';
import 'portfolios_tab.dart' show parseColor;
import 'settings_tab.dart';
import 'smart_home_screen.dart';
import 'widgets.dart';

/// What the app knows about a dashboard tile's `screen` (from the gateway's app_features table): the page to
/// open, an icon, and the title/description in the app's language. Tiles with an unknown screen still show
/// the server's texts and open a "not available yet" page.
class FeatureScreen {
  const FeatureScreen(this.icon, this.page, this.title, this.description);

  final IconData icon;
  final Widget Function() page;
  final String Function(AppLocalizations) title;
  final String Function(AppLocalizations) description;
}

final Map<String, FeatureScreen> featureScreens = {
  'Portfolio': FeatureScreen(Icons.trending_up, () => const InvestmentsScreen(), (t) => t.featurePortfolio, (t) => t.featurePortfolioDesc),
  'Transactions': FeatureScreen(Icons.account_balance_wallet_outlined, () => const MoneyScreen(), (t) => t.featureTransactions, (t) => t.featureTransactionsDesc),
  'MedicalHistory': FeatureScreen(Icons.medical_services_outlined, () => const MedicalScreen(), (t) => t.featureMedical, (t) => t.featureMedicalDesc),
  'SmartHome': FeatureScreen(Icons.home_outlined, () => const SmartHomeScreen(), (t) => t.featureSmartHome, (t) => t.featureSmartHomeDesc),
  'UpdateUserInfo': FeatureScreen(Icons.person_outline, () => const SettingsTab(), (t) => t.featureAccount, (t) => t.featureAccountDesc),
};

class DashboardScreen extends StatelessWidget {
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final session = context.read<Session>();
    final name = session.user!.fullName.trim();
    return Scaffold(
      appBar: AppBar(
        title: Text(t.appTitle),
        actions: [
          IconButton(
            tooltip: t.settings,
            icon: const Icon(Icons.settings_outlined),
            onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const SettingsTab())),
          ),
        ],
      ),
      body: Loader<List<Feature>>(
        load: session.api.features,
        builder: (context, features, reload) => CustomScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          slivers: [
            if (name.isNotEmpty)
              SliverPadding(
                padding: const EdgeInsets.fromLTRB(16, 16, 16, 0),
                sliver: SliverToBoxAdapter(
                  child: Text(t.hello(name.split(' ').first), style: Theme.of(context).textTheme.headlineSmall),
                ),
              ),
            SliverPadding(
              padding: const EdgeInsets.all(12),
              sliver: SliverLayoutBuilder(builder: (context, constraints) {
                // rows of equal-height tiles that grow with their text (large font sizes, long Arabic texts)
                final columns = (constraints.crossAxisExtent / 220).floor().clamp(2, 4);
                final rows = (features.length / columns).ceil();
                return SliverList.builder(
                  itemCount: rows,
                  itemBuilder: (c, row) => IntrinsicHeight(
                    child: Row(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
                      for (var i = row * columns; i < (row + 1) * columns; i++)
                        Expanded(child: i < features.length ? FeatureTile(feature: features[i]) : const SizedBox()),
                    ]),
                  ),
                );
              }),
            ),
          ],
        ),
      ),
    );
  }
}

class FeatureTile extends StatelessWidget {
  const FeatureTile({super.key, required this.feature});

  final Feature feature;

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final known = featureScreens[feature.screen];
    final scheme = Theme.of(context).colorScheme;
    final color = parseColor(feature.color) ?? scheme.primary;
    final title = known?.title(t) ?? feature.displayName;
    final description = known?.description(t) ?? feature.description ?? '';
    return Card(
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () => Navigator.push(
          context,
          MaterialPageRoute(builder: (_) => known?.page() ?? _NotInApp(title: title)),
        ),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, mainAxisSize: MainAxisSize.min, children: [
            Container(
              width: 52,
              height: 52,
              decoration: BoxDecoration(color: color.withValues(alpha: 0.15), borderRadius: BorderRadius.circular(14)),
              alignment: Alignment.center,
              child: known != null || (feature.icon ?? '').isEmpty
                  ? Icon(known?.icon ?? Icons.apps, color: color, size: 28)
                  : Text(feature.icon!, style: const TextStyle(fontSize: 26)), // the server's emoji
            ),
            const SizedBox(height: 16),
            Text(title, style: Theme.of(context).textTheme.titleMedium, maxLines: 2, overflow: TextOverflow.ellipsis),
            const SizedBox(height: 4),
            Text(
              description,
              style: Theme.of(context).textTheme.bodySmall?.copyWith(color: scheme.onSurfaceVariant),
            ),
          ]),
        ),
      ),
    );
  }
}

class _NotInApp extends StatelessWidget {
  const _NotInApp({required this.title});

  final String title;

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: Text(title)),
        body: EmptyState(icon: Icons.construction_outlined, text: AppLocalizations.of(context).featureNotInApp),
      );
}
