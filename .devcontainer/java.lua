-- Copyright DB InfraGO AG and contributors
-- SPDX-License-Identifier: Apache-2.0

-- https://github.com/mfussenegger/nvim-dap/wiki/Java
local dap = require('dap')

local project_name = vim.fn.fnamemodify(vim.fn.getcwd(), ':p:h:t')
local workspaces_dir = '/workspaces/'
local workspace_dir = workspaces_dir .. project_name

dap.configurations.java = {
    {
        name = "Eclipse Plugin",
        request = "launch",
        type = "java",
        javaExec = "java",
        args =
        "-product org.polarsys.capella.rcp.product -launcher /opt/capella/capella -name Eclipse -data /workspaces/RUNTIME --add-modules=ALL-SYSTEM -os linux -ws gtk -arch " .. vim.fn.system("uname -m") .. " -nl en_US -clean -consoleLog -debug",
        vmArgs =
        "-XX:+ShowCodeDetailsInExceptionMessages -Dorg.eclipse.swt.graphics.Resource.reportNonDisposed=true -Declipse.pde.launch=true -Dfile.encoding=UTF-8",
        classPaths = {
            "/opt/capella/plugins/org.eclipse.equinox.launcher_1.6.200.v20210416-2027.jar",
        },
        mainClass = "org.eclipse.equinox.launcher.Main",
        env = {
            -- MODEL_INBOX_DIRECTORIES = "/dev/github/capella-addons/data/models/empty_capella_project:/dev/github/capella-addons/data/models/test:/dev/github/capella-rest-api/data/models/automated-train",
            -- SELECTED_ELEMENTS_SERVICE_TARGET_URL = "http://localhost:8080",
        }
    },
}


local jdtls = require('jdtls')
local extendedClientCapabilities = jdtls.extendedClientCapabilities
extendedClientCapabilities.resolveAdditionalTextEditsSupport = true
local bundles = {
    vim.fn.glob("/opt/com.microsoft.java.debug.plugin-*.jar", 1),
}
local config = {
    cmd = {
        'java',
        '-Declipse.application=org.eclipse.jdt.ls.core.id1',
        '-Dosgi.bundles.defaultStartLevel=4',
        '-Declipse.product=org.eclipse.jdt.ls.core.product',
        '-Dlog.protocol=true',
        '-Dlog.level=ALL',
        '-Xmx1g',
        '--add-modules=ALL-SYSTEM',
        '--add-opens', 'java.base/java.util=ALL-UNNAMED',
        '--add-opens', 'java.base/java.lang=ALL-UNNAMED',
        '-jar',
        '/opt/eclipse.jdt.ls/plugins/org.eclipse.equinox.launcher_1.6.800.v20240304-1850.jar',
        '-configuration', '/opt/eclipse.jdt.ls/config_linux_arm',
        '-data', workspace_dir,
    },
    root_dir = vim.fs.dirname(vim.fs.find({ 'plugin.xml', 'pom.xml', '.project' }, { upward = true })[1]),
    settings = {
        java = {
            configuration = {
                -- See https://github.com/eclipse/eclipse.jdt.ls/wiki/Running-the-JAVA-LS-server-from-the-command-line#initialize-request
                -- And search for `interface RuntimeOption`
                -- The `name` is NOT arbitrary, but must match one of the elements from `enum ExecutionEnvironment` in the link above
                runtimes = {
                    {
                        name = "JavaSE-11",
                        path = "/usr/lib/jvm/java-11-openjdk/",
                    },
                    {
                        name = "JavaSE-17",
                        path = "/usr/lib/jvm/java-17-openjdk/",
                    }
                }
            },
            -- updateBuildConfiguration = "automatic",
            signatureHelp = { enabled = true },
        }
    },
    capabilities = require("cmp_nvim_lsp").default_capabilities(),
    handlers = {
        ['textDocument/documentSymbol'] = function(_, result, ctx, _)
            local bufnr =
                vim.uri_to_bufnr(vim.uri_from_fname(ctx.params.textDocument.uri))

            local items = {}
            local symbols = {}
            for _, symbol in ipairs(result) do
                -- print(symbol.kind)
                print(vim.inspect(symbol))
                local range = symbol.location and symbol.location.range or symbol.range
                local start = range.start
                table.insert(symbols, symbol)
                table.insert(items, {
                    bufnr = bufnr,
                    lnum = start.line + 1,
                    col = start.character + 1,
                    text = symbol.name,
                    type = 'I', -- I for Information
                })
            end
            items = vim.lsp.util.symbols_to_items(symbols)

            -- Update quickfix list with the items
            -- see `:h setqflist`
            vim.fn.setqflist({}, 'r', {
                title = 'Document Symbols',
                items = items,
                quickfixtextfunc = function(info)
                    local custom_items = vim.fn.getqflist({ id = info.id, items = 1 }).items
                    local l = {}
                    local txt = ''
                    for idx = info.start_idx, info.end_idx do
                        -- table.insert(l, vim.fn.fnamemodify(vim.fn.bufname(custom_items[idx].bufnr), ':p:.'))
                        -- see `:h quickfix-window-function`
                        txt = custom_items[idx].text
                        -- see https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#symbolKind
                        txt = txt:gsub('%[Class%]', ' ')
                        txt = txt:gsub('%[Constant%]', '  ')
                        txt = txt:gsub('%[Constructor%]', ' ')
                        txt = txt:gsub('%[Enum%]', '  ')
                        txt = txt:gsub('%[EnumMember%]', '  ')
                        txt = txt:gsub('%[Event%]', ' ')
                        txt = txt:gsub('%[Field%]', ' ')
                        txt = txt:gsub('%[File%]', ' ')
                        txt = txt:gsub('%[Function%]', ' ')
                        txt = txt:gsub('%[Interface%]', ' ')
                        txt = txt:gsub('%[Key%]', ' ')
                        txt = txt:gsub('%[Method%]', ' ')
                        txt = txt:gsub('%[Module%]', ' ')
                        txt = txt:gsub('%[Namespace%]', ' ')
                        txt = txt:gsub('%[Number%]', ' ')
                        txt = txt:gsub('%[Object%]', ' ')
                        txt = txt:gsub('%[Operator%]', ' ')
                        txt = txt:gsub('%[Package%]', ' ')
                        txt = txt:gsub('%[Property%]', ' ')
                        txt = txt:gsub('%[String%]', ' ')
                        txt = txt:gsub('%[Struct%]', ' ')
                        txt = txt:gsub('%[TypeParameter%]', ' ')
                        txt = txt:gsub('%[Variable%]', ' ')
                        table.insert(l, txt)
                    end
                    return l
                end
            })
        end,
    },
    init_options = {
        bundles = {
            vim.fn.glob("/opt/com.microsoft.java.debug.plugin-*.jar", 1)
        },
    }
}
jdtls.start_or_attach(config)
