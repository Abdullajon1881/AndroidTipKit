// Metro config for the monorepo example. It consumes the library directly from
// source (`../src`) — no build or npm link needed — and resolves react /
// react-native from this example's own node_modules.
const { getDefaultConfig } = require('expo/metro-config');
const path = require('path');

const projectRoot = __dirname;
const packageSrc = path.resolve(projectRoot, '..', 'src');

const config = getDefaultConfig(projectRoot);

// Watch the library source so edits hot-reload.
config.watchFolders = [packageSrc];

// Resolve the package name to source, and shared peers to this app's copies.
config.resolver.extraNodeModules = {
  '@abdullajon1991/nudgekit-react-native': packageSrc,
  react: path.resolve(projectRoot, 'node_modules', 'react'),
  'react-native': path.resolve(projectRoot, 'node_modules', 'react-native'),
};
config.resolver.nodeModulesPaths = [path.resolve(projectRoot, 'node_modules')];

module.exports = config;
