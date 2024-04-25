-- Copyright DB InfraGO AG and contributors
-- SPDX-License-Identifier: Apache-2.0

return {
    {
        -- https://github.com/tpope/vim-commentary
        "tpope/vim-commentary",
        event = "VeryLazy"
    },
    {
        -- https://github.com/tpope/vim-fugitive
        "tpope/vim-fugitive",
        cmd = { "G", "Gclog" },
        event = "VeryLazy"
    },
    {
        -- https://github.com/tpope/vim-repeat
        "tpope/vim-repeat",
        event = "VeryLazy"
    },
    {
        -- https://github.com/neovim/nvim-lspconfig
        "neovim/nvim-lspconfig",
        event = "VeryLazy",
        config = function() require("config.nvim-lspconfig") end,
        enabled = function()
            return os.getenv("HOSTNAME") == "devcontainer"
        end
    },
    {
        -- https://github.com/tpope/vim-surround
        -- (e. g. cs"' to replace double by single quotes)
        "tpope/vim-surround",
        event = "VeryLazy"
    },
    {
        -- https://github.com/nvim-treesitter/nvim-treesitter
        "nvim-treesitter/nvim-treesitter",
        lazy = false,
        build = ":TSUpdate",
        config = function()
            require "nvim-treesitter.configs".setup {
                ensure_installed = "java",
                highlight = { enable = true }
            }
        end
    },
    {
        -- https://github.com/mfussenegger/nvim-dap
        "mfussenegger/nvim-dap",
        ft = { "java" },
        config = function()
            local dap = require('dap')
            dap.defaults.fallback.switchbuf = 'usetab'
            dap.defaults.fallback.terminal_win_cmd = 'belowright 15new | wincmd J'
            vim.fn.sign_define('DapBreakpoint',
                { text = '', texthl = 'DapBreakpointText', linehl = 'DapBreakpointLine', numhl = '' })
            vim.fn.sign_define('DapStopped',
                { text = '', texthl = 'DapStoppedText', linehl = 'DapStoppedLine', numhl = '' })
        end
    },
    {
        -- https://github.com/mfussenegger/nvim-jdtls
        "mfussenegger/nvim-jdtls",
        -- https://github.com/mfussenegger/nvim-dap
        dependencies = "mfussenegger/nvim-dap",
        ft = "java"
    },
    {
        -- https://github.com/hrsh7th/nvim-cmp
        "hrsh7th/nvim-cmp", -- ENGINE
        event = "InsertEnter",
        config = function() require("config.nvim-cmp") end,
        dependencies = {
            -- SOURCES/ PROVIDERS
            -- (sources are the bridge between provider and nvim-cmp):
            {
                -- https://github.com/hrsh7th/cmp-buffer
                "hrsh7th/cmp-buffer",
            },
            {
                -- https://github.com/hrsh7th/cmp-nvim-lsp
                "hrsh7th/cmp-nvim-lsp",                    -- source
                dependencies = { "neovim/nvim-lspconfig" } -- provider
            },
            {
                -- https://github.com/hrsh7th/cmp-nvim-lsp-signature-help
                "hrsh7th/cmp-nvim-lsp-signature-help",     -- source
                dependencies = { "neovim/nvim-lspconfig" } -- provider
            },
            {
                -- https://github.com/hrsh7th/cmp-path
                "hrsh7th/cmp-path", -- source
            },
            { "kyazdani42/nvim-web-devicons" }
        }
    },
}
