// 导出所有提示词相关的API
// 导入模块
import * as answerTagPromptModule from './answerTagPrompt'
import * as answerTypePromptModule from './answerTypePrompt'

// 导出函数API，只导出函数避免命名冲突
export const createAnswerTagPrompt = answerTagPromptModule.createAnswerTagPrompt
export const updateAnswerTagPrompt = answerTagPromptModule.updateAnswerTagPrompt
export const getAnswerTagPromptDetail = answerTagPromptModule.getAnswerTagPromptDetail
export const getAllAnswerTagPrompts = answerTagPromptModule.getAllAnswerTagPrompts
export const getActiveAnswerTagPromptsByTagId = answerTagPromptModule.getActiveAnswerTagPromptsByTagId
export const deleteAnswerTagPrompt = answerTagPromptModule.deleteAnswerTagPrompt

// 导出类型，使用类型导出语法
export type AnswerTagPrompt = answerTagPromptModule.AnswerTagPrompt
export type AnswerTagPromptCreateData = answerTagPromptModule.AnswerTagPromptCreateData
export type AnswerTagPromptUpdateData = answerTagPromptModule.AnswerTagPromptUpdateData
export type DeleteAnswerTagPromptData = answerTagPromptModule.DeleteAnswerTagPromptData
export type Tag = answerTagPromptModule.Tag
// 重命名有冲突的类型
export type AnswerTagUserInfo = answerTagPromptModule.UserInfo

// 导出 answerTypePrompt 模块的函数API
export const createAnswerTypePrompt = answerTypePromptModule.createAnswerTypePrompt
export const updateAnswerTypePrompt = answerTypePromptModule.updateAnswerTypePrompt
export const getAnswerTypePromptDetail = answerTypePromptModule.getAnswerTypePromptDetail
export const getAllAnswerTypePrompts = answerTypePromptModule.getAllAnswerTypePrompts
export const getActiveAnswerTypePromptsByType = answerTypePromptModule.getActiveAnswerTypePromptsByType
export const deleteAnswerTypePrompt = answerTypePromptModule.deleteAnswerTypePrompt

// 导出 answerTypePrompt 模块的类型
export type AnswerTypePrompt = answerTypePromptModule.AnswerTypePrompt
export type AnswerTypePromptCreateData = answerTypePromptModule.AnswerTypePromptCreateData
export type AnswerTypePromptUpdateData = answerTypePromptModule.AnswerTypePromptUpdateData
export type DeleteAnswerTypePromptData = answerTypePromptModule.DeleteAnswerTypePromptData
// 导出枚举类型
export { QuestionType } from './answerTypePrompt'
// 重命名有冲突的类型
export type AnswerTypeUserInfo = answerTypePromptModule.UserInfo

// 导出answerPromptAssemblyConfig中的所有内容
export * from './answerPromptAssemblyConfig'
