-- Copyright DB InfraGO AG and contributors
-- SPDX-License-Identifier: Apache-2.0

local cmp = require("cmp")

local cmp_kinds = {
    Class = "  ",
    Color = "  ",
    Constant = "  ",
    Constructor = "  ",
    Enum = "  ",
    EnumMember = "  ",
    Event = "  ",
    Field = "  ",
    File = "  ",
    Folder = "  ",
    Function = "  ",
    Interface = "  ",
    Keyword = "  ",
    Method = "  ",
    Module = "  ",
    Operator = "  ",
    Property = "  ",
    Reference = "  ",
    Snippet = "  ",
    Struct = "  ",
    Text = "  ",
    TypeParameter = "  ",
    Unit = "  ",
    Value = "  ",
    Variable = "  ",
}
local has_words_before = function()
    local line, col = unpack(vim.api.nvim_win_get_cursor(0))
    return col ~= 0 and vim.api.nvim_buf_get_lines(0, line - 1, line, true)[1]:sub(col, col):match("%s") == nil
end

local feedkey = function(key, mode)
    vim.api.nvim_feedkeys(vim.api.nvim_replace_termcodes(key, true, true, true), mode, true)
end

cmp.setup({
    formatting = {
        -- https://github.com/hrsh7th/nvim-cmp/wiki/Menu-Appearance#how-to-add-visual-studio-code-codicons-to-the-menu
        format = function(_, vim_item)
            vim_item.kind = (cmp_kinds[vim_item.kind] or "") .. vim_item.kind
            return vim_item
        end,
    },
    -- window = {
    -- completion = cmp.config.window.bordered(),
    -- documentation = cmp.config.window.bordered(),
    -- },
    sources = cmp.config.sources({
        {
            name = "buffer",
            option = {
                keyword_pattern = [[\K\k*]],
            },
        },
        { name = "nvim_lsp" },
        { name = "nvim_lsp_document_symbol" },
        { name = "nvim_lsp_signature_help" },
        {
            name = "path",
            option = {
                trailing_slash = true
            }
        },
    }),
    mapping = cmp.mapping.preset.insert({
        ["<Tab>"] = cmp.mapping(function(fallback)
            if cmp.visible() then
                cmp.select_next_item()
            elseif vim.fn["vsnip#available"](1) == 1 then
                feedkey("<Plug>(vsnip-expand-or-jump)", "")
            elseif has_words_before() then
                cmp.complete()
            else
                fallback() -- The fallback function sends an already mapped key. In this case, it's probably `<Tab>`.
            end
            -- end, { "i", "s", "t" }),
        end, { "i", "s" }),
        ["<S-Tab>"] = cmp.mapping(function()
            if cmp.visible() then
                cmp.select_prev_item()
            elseif vim.fn["vsnip#jumpable"](-1) == 1 then
                feedkey("<Plug>(vsnip-jump-prev)", "")
            end
            -- end, { "i", "s", "t" }),
        end, { "i", "s" }),
        ['<C-b>'] = cmp.mapping.scroll_docs(-4),
        ['<C-f>'] = cmp.mapping.scroll_docs(4),
        ['<C-Space>'] = cmp.mapping.complete(),
        ['<C-e>'] = cmp.mapping.abort(),
        ['<CR>'] = cmp.mapping.confirm({ select = true }), -- Accept currently selected item. Set `select` to `false` to only confirm explicitly selected items.
    })
})
