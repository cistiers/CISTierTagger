"""Builds the release workflow matrix from selected Stonecutter targets"""

import json
import os
import tomllib

selected = os.environ['SELECTED'].replace(',', ' ').split()
selected = {'1.20.1' if value == '1.20' else value for value in selected}
targets = []
with open('stonecutter.properties.toml', 'rb') as source:
    config = tomllib.load(source)
for mc, target in sorted(config.items()):
    if 'mod' not in target:
        continue
    properties = config['mod'] | target['mod']
    targets.append({
        'mc': mc,
        'java': str(properties['java_version']),
        'game_versions': ' '.join(properties['game_versions']),
    })
unknown = selected - {target['mc'] for target in targets} - {'all'}
if unknown:
    raise ValueError('Unknown targets: ' + ', '.join(sorted(unknown)))
if 'all' in selected and len(selected) > 1:
    raise ValueError('Use all without individual targets')
matrix = {'include': [
    target for target in targets
    if not selected or selected == {'all'} or target['mc'] in selected
]}
with open(os.environ['GITHUB_OUTPUT'], 'a') as output:
    output.write('matrix=' + json.dumps(matrix, separators=(',', ':')) + '\n')
