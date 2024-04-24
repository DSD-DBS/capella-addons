colorscheme slate
set shell=/bin/zsh
let g:mapleader="ö"
set colorcolumn=79
set completeopt="menuone,noinsert,noselect,preview"
set cursorline
set cursorlineopt=number
set expandtab  " blanks instead of tab
set foldexpr=nvim_treesitter#foldexpr()
" search:
set ignorecase " case insensitive search
set smartcase  " but become case-sensitive if you type uppercase characters
" Highlight trailing whitespace
set list listchars=tab:→\ ,trail:·,extends:>,precedes:<
set mouse=a
set nobackup
set noswapfile
set nowrap
set nowritebackup
set number
set relativenumber
set shiftwidth=4
set tabstop=4  " tab width
set termguicolors  " needed by plugin feline
set wildmenu  " display all matching files when we tab complete
lua require("init")
" LSP
nnoremap <silent><leader>lc <cmd>lua vim.lsp.buf.declaration()<cr>
nnoremap <silent><leader>ld <cmd>lua vim.lsp.buf.definition()<cr> z<cr>
nnoremap <silent><leader>lf :lua vim.lsp.buf.format({timeout_ms = 5000})<cr>
nnoremap <silent><leader>lh <cmd>lua vim.lsp.buf.hover()<cr>
nnoremap <silent><leader>li <cmd>lua vim.lsp.buf.implementation()<cr>
nnoremap <silent><leader>lj <cmd>lua vim.diagnostic.goto_next{wrap=false,popup_opts={border="single"}}<cr>
nnoremap <silent><leader>lk <cmd>lua vim.diagnostic.goto_prev{wrap=false,popup_opts={border="single"}}<cr>
nnoremap <silent><leader>ln <cmd>lua vim.lsp.buf.rename()<cr>
nnoremap <silent><leader>lsd <cmd>lua vim.lsp.buf.document_symbol()<cr><cmd>copen<cr>
nnoremap <silent><leader>lsw <cmd>lua vim.lsp.buf.workspace_symbol()<cr>
nnoremap <silent><leader>lt <cmd>lua vim.lsp.buf.type_definition()<cr>
nnoremap <silent><leader>lS <cmd>lua require('jdtls').super_implementation()<cr>
nnoremap <silent><leader>lp <cmd>lua vim.diagnostic.open_float(nil, {scope = "line", focus = true, focusable = true, focus_id = "1"})<cr>
nnoremap <silent><leader>lP <cmd>lua vim.diagnostic.open_float(nil, {scope = "buffer", focus = true, focusable = true})<cr>
nnoremap <silent><leader>lr <cmd>lua vim.lsp.buf.references()<cr>

function! PackageAndDeployEclipsePlugin()
    silent! !python3 -c "import time; time.sleep(1)"
    !python3 /opt/eclipse_plugin_builders.py package
    !python3 /opt/eclipse_plugin_builders.py deploy /opt/capella
endfunction
function! CompilePackageAndDeployEclipsePlugin()
    JdtCompile full
    silent! !python3 -c "import time; time.sleep(1)"
    call PackageAndDeployEclipsePlugin()
    silent! !python3 -c "import time; time.sleep(2)"
endfunction

autocmd! BufWritePost *.java lua vim.defer_fn(function() vim.cmd('JdtUpdateHotcode') end, 100)
autocmd! BufWritePost MANIFEST.MF call PackageAndDeployEclipsePlugin()
autocmd! TermOpen * setlocal colorcolumn=0 nomodified nonumber ruler norelativenumber
" leave insert mode of terminal as it is in vim
tnoremap <c-w>N <c-\><c-n>

" nvim-dap (see :h :dap.txt)
" dap mostly opening new windows
nnoremap <silent><leader>Db <cmd>lua require('dap').list_breakpoints()<cr><cmd>copen<cr>
nnoremap <silent><leader>Df <cmd>lua require('dap.ui.widgets').sidebar(require('dap.ui.widgets').frames).open()<cr>
nnoremap <silent><leader>dh <cmd>lua require('dap.ui.widgets').hover()<cr>
nnoremap <silent><leader>Dr <cmd>lua require('dap').repl.open()<cr>
nnoremap <silent><leader>Ds <cmd>lua require('dap.ui.widgets').sidebar(require('dap.ui.widgets').scopes).open()<cr>

" fallbacks if function keys do not work:
nnoremap <silent><leader>db <cmd>lua require('dap').toggle_breakpoint()<cr>
nnoremap <silent><leader>dr <cmd>lua require('dap').restart()<cr>
" continue functions also as (re-)start
nnoremap <silent><leader>dc <cmd>lua require('dap').continue()<cr>
nnoremap <silent><leader>dC <cmd>lua require('dap').run_last()<cr>
nnoremap <silent><leader>d<esc> <cmd>lua require('dap').terminate()<cr>

nnoremap <silent><leader>do <cmd>lua require('dap').step_out()<cr>
nnoremap <silent><leader>dn <cmd>lua require('dap').step_over()<cr>
nnoremap <silent><leader>ds <cmd>lua require('dap').step_into()<cr>
nnoremap <silent><leader>dx <cmd>lua require('dap').clear_breakpoints()<cr>
" java (eclipse.jdt.ls)
nnoremap <silent><leader>jb <cmd>lua require('jdtls').build_projects({select_mode='all', full_build=false})<cr>
nnoremap <silent><leader>jc <cmd>lua require('jdtls').compile('full')<cr>
nnoremap <silent><leader>jC <cmd>call CompilePackageAndDeployEclipsePlugin()<cr>
nnoremap <silent><leader>jh <cmd>JdtUpdateHotcode<cr>
nnoremap <silent><leader>jo <cmd>lua require('jdtls').organize_imports()<cr>
nnoremap <silent><leader>jp <cmd>!python3 /opt/eclipse_plugin_builders.py build-classpath /opt/capella<cr>

" Clear search highlighting
nnoremap <silent><leader>ö <cmd>nohlsearch<cr>

" line numbers
noremap <silent><leader>a <cmd>setlocal norelativenumber number<cr>
noremap <silent><leader>r <cmd>setlocal relativenumber<cr>
noremap <silent><leader>n <cmd>setlocal norelativenumber nonumber<cr>
