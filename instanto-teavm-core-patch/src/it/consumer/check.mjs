import {pathToFileURL} from 'node:url';

const {main} = await import(pathToFileURL(process.argv[2]));
await new Promise((resolve, reject) => main([], result => result instanceof Error ? reject(result) : resolve()));
console.log('Class initialization application passed');
