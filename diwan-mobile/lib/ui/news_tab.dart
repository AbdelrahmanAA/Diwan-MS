import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:url_launcher/url_launcher.dart';

import '../api/models.dart';
import '../l10n/app_localizations.dart';
import '../state/session.dart';
import 'format.dart';
import 'widgets.dart';

/// News about the customer's investments and watchlist, newest first.
class NewsTab extends StatelessWidget {
  const NewsTab({super.key});

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final api = context.read<Session>().api;
    return Scaffold(
      appBar: AppBar(title: Text(t.news)),
      body: Loader<ResultPage<NewsItem>>(
        load: () => api.news(),
        builder: (context, page, reload) {
          if (page.items.isEmpty) return EmptyState(icon: Icons.newspaper_outlined, text: t.noNews);
          final f = Fmt(context);
          return ListView.separated(
            itemCount: page.items.length,
            separatorBuilder: (_, _) => const Divider(height: 1),
            itemBuilder: (c, i) {
              final n = page.items[i];
              return ListTile(
                isThreeLine: n.summary != null,
                leading: CircleAvatar(child: Text(n.symbol.characters.take(2).toString(), style: const TextStyle(fontSize: 12))),
                // provider news is English: keep it left-to-right even in the Arabic UI
                title: Text(n.headline, textDirection: TextDirection.ltr, maxLines: 2, overflow: TextOverflow.ellipsis),
                subtitle: Text(
                  [if (n.source != null) n.source!, f.dateTime(n.publishedAt), if (n.summary != null) '\n${n.summary}'].join(' · '),
                  maxLines: 3,
                  overflow: TextOverflow.ellipsis,
                ),
                onTap: () => launchUrl(Uri.parse(n.url), mode: LaunchMode.externalApplication),
              );
            },
          );
        },
      ),
    );
  }
}
