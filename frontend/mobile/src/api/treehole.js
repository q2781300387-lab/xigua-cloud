import { core } from '@/core/index.js'

export function treeholeList(params) {
	return core.treehole.treeholeList(params)
}

export function treeholeAdd(data) {
	return core.treehole.treeholeAdd(data)
}

export function treeholeLike(id) {
	return core.treehole.treeholeLike(id)
}
