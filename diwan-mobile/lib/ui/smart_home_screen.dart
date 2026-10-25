import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/models.dart';
import '../l10n/app_localizations.dart';
import '../state/session.dart';
import 'widgets.dart';

/// Smart home (diwan-smarthome): the user's devices with an on/off switch (sent to the device over MQTT by
/// the server), plus add / rename / remove.
class SmartHomeScreen extends StatefulWidget {
  const SmartHomeScreen({super.key});

  @override
  State<SmartHomeScreen> createState() => _SmartHomeScreenState();
}

class _SmartHomeScreenState extends State<SmartHomeScreen> {
  Key _key = UniqueKey();

  void _refresh() => setState(() => _key = UniqueKey());

  Future<String?> _askName(String title, {String initial = ''}) {
    final t = AppLocalizations.of(context);
    final name = TextEditingController(text: initial);
    return showDialog<String>(
      context: context,
      builder: (c) => AlertDialog(
        title: Text(title),
        content: TextField(controller: name, autofocus: true, decoration: InputDecoration(labelText: t.name)),
        actions: [
          TextButton(onPressed: () => Navigator.pop(c), child: Text(t.cancel)),
          FilledButton(onPressed: () => Navigator.pop(c, name.text.trim()), child: Text(t.save)),
        ],
      ),
    );
  }

  Future<void> _add() async {
    final added = await showModalBottomSheet<bool>(
        context: context, isScrollControlled: true, builder: (_) => const _AddDeviceSheet());
    if (added == true) _refresh();
  }

  Future<void> _rename(SmartDevice d) async {
    final name = await _askName(AppLocalizations.of(context).rename, initial: d.name);
    if (name == null || name.isEmpty || !mounted) return;
    try {
      await context.read<Session>().api.renameDevice(d.id, name);
      _refresh();
    } catch (e) {
      if (mounted) showError(context, e);
    }
  }

  Future<void> _remove(SmartDevice d) async {
    if (!await confirm(context, AppLocalizations.of(context).confirmDelete) || !mounted) return;
    try {
      await context.read<Session>().api.removeDevice(d.id);
      _refresh();
    } catch (e) {
      if (mounted) showError(context, e);
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    return Scaffold(
      appBar: AppBar(title: Text(t.featureSmartHome)),
      floatingActionButton: FloatingActionButton.extended(
        icon: const Icon(Icons.add),
        label: Text(t.addDevice),
        onPressed: _add,
      ),
      body: Loader<List<SmartDevice>>(
        key: _key,
        load: context.read<Session>().api.devices,
        builder: (context, list, reload) {
          if (list.isEmpty) return EmptyState(icon: Icons.lightbulb_outline, text: t.noDevices);
          return ListView(padding: const EdgeInsets.fromLTRB(12, 12, 12, 96), children: [
            for (final d in list)
              _DeviceCard(
                key: ValueKey(d.id),
                device: d,
                onRename: () => _rename(d),
                onRemove: () => _remove(d),
              ),
            Padding(
              padding: const EdgeInsets.all(16),
              child: Text(t.alexaHint,
                  textAlign: TextAlign.center,
                  style: TextStyle(color: Theme.of(context).colorScheme.onSurfaceVariant)),
            ),
          ]);
        },
      ),
    );
  }
}

class _DeviceCard extends StatefulWidget {
  const _DeviceCard({super.key, required this.device, required this.onRename, required this.onRemove});

  final SmartDevice device;
  final VoidCallback onRename;
  final VoidCallback onRemove;

  @override
  State<_DeviceCard> createState() => _DeviceCardState();
}

class _DeviceCardState extends State<_DeviceCard> {
  late SmartDevice _device = widget.device;
  bool _busy = false;

  @override
  void didUpdateWidget(_DeviceCard old) {
    super.didUpdateWidget(old);
    _device = widget.device; // reloaded list (after a rename)
  }

  Future<void> _switch(bool on) async {
    setState(() => _busy = true);
    try {
      final updated = await context.read<Session>().api.switchDevice(_device.id, on);
      if (mounted) setState(() => _device = updated);
    } catch (e) {
      if (mounted) showError(context, e);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final d = _device;
    final state = switch (d.state) { 'ON' => t.deviceOn, 'OFF' => t.deviceOff, _ => t.deviceUnknown };
    return Card(
      child: ListTile(
        contentPadding: const EdgeInsetsDirectional.only(start: 16, end: 4),
        leading: Icon(d.isOn ? Icons.lightbulb : Icons.lightbulb_outline,
            size: 32, color: d.isOn ? Colors.amber.shade600 : Theme.of(context).colorScheme.outline),
        title: Text(d.name),
        subtitle: Text('$state · ${d.deviceId}'),
        trailing: Row(mainAxisSize: MainAxisSize.min, children: [
          _busy
              ? const Padding(
                  padding: EdgeInsets.symmetric(horizontal: 16),
                  child: SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2)),
                )
              : Switch(value: d.isOn, onChanged: _switch),
          PopupMenuButton<String>(
            onSelected: (v) => v == 'rename' ? widget.onRename() : widget.onRemove(),
            itemBuilder: (_) => [
              PopupMenuItem(value: 'rename', child: Text(t.rename)),
              PopupMenuItem(value: 'remove', child: Text(t.remove)),
            ],
          ),
        ]),
      ),
    );
  }
}

class _AddDeviceSheet extends StatefulWidget {
  const _AddDeviceSheet();

  @override
  State<_AddDeviceSheet> createState() => _AddDeviceSheetState();
}

class _AddDeviceSheetState extends State<_AddDeviceSheet> {
  final _form = GlobalKey<FormState>();
  final _deviceId = TextEditingController();
  final _name = TextEditingController();
  bool _busy = false;

  Future<void> _save() async {
    if (!_form.currentState!.validate()) return;
    setState(() => _busy = true);
    try {
      await context.read<Session>().api.addDevice(_deviceId.text.trim(), _name.text.trim());
      if (mounted) Navigator.pop(context, true);
    } catch (e) {
      if (mounted) showError(context, e);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    String? required(String? v) => v == null || v.trim().isEmpty ? t.required : null;
    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
      child: Form(
        key: _form,
        child: ListView(shrinkWrap: true, padding: const EdgeInsets.all(16), children: [
          Text(t.addDevice, style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: 12),
          TextFormField(
            controller: _deviceId,
            autofocus: true,
            textDirection: TextDirection.ltr,
            decoration: InputDecoration(labelText: t.deviceId, helperText: t.deviceIdHint),
            validator: required,
          ),
          TextFormField(controller: _name, decoration: InputDecoration(labelText: t.name), validator: required),
          const SizedBox(height: 24),
          FilledButton(
            onPressed: _busy ? null : _save,
            child: _busy
                ? const SizedBox(height: 20, width: 20, child: CircularProgressIndicator(strokeWidth: 2))
                : Text(t.save),
          ),
        ]),
      ),
    );
  }
}
