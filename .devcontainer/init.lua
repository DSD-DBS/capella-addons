-- Copyright DB InfraGO AG and contributors
-- SPDX-License-Identifier: Apache-2.0
-- Lazy {{{
local lazypath = vim.fn.stdpath("data") .. "/lazy/lazy.nvim"
if not vim.loop.fs_stat(lazypath) then
    vim.fn.system({
        "git",
        "clone",
        "--filter=blob:none",
        "https://github.com/folke/lazy.nvim.git",
        "--branch=stable", -- latest stable release
        lazypath,
    })
end
vim.opt.rtp:prepend(lazypath)
require("plugins")
require("lazy").setup("plugins", {
    defaults = { lazy = true },
    ui = {
        border = "rounded",
        size = { width = 0.6, height = 0.9 },
    }
})
-- }}}
