vim.lsp.set_log_level("warn")

vim.diagnostic.config({
    signs = true,
    underline = true,
    update_in_insert = true,
    float = {
        focusable = true,
        focus = true,
        severity_sort = true,
        source = "always"
    },
    severity_sort = true,
    source = true,
    virtual_text = false
})

local signs = { Error = " ", Warn = " ", Hint = " ", Info = " " }
for type, icon in pairs(signs) do
    local hl = "DiagnosticSign" .. type
    vim.fn.sign_define(hl, { text = icon, texthl = hl, numhl = hl })
end
