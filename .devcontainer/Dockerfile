# Copyright DB InfraGO AG and contributors
# SPDX-License-Identifier: Apache-2.0

FROM fedora:38
LABEL maintainer "Jamil André RAICHOUNI <jamil.raichouni@deutschebahn.com>"

# system pkg manager installs {{{
RUN dnf --color yes --refresh -y install \
  # to clone bespoke eclipse-plugin-builders
  gcc \
  git \
  # to develop Capella 5.x plugins
  java-11-openjdk-devel \
  # to develop Capella 6.x plugins
  java-17-openjdk-devel \
  maven \
  python3-pip \
  zsh
# }}}
# configure terminal {{{
SHELL ["/bin/zsh", "-c"]
ENV SHELL=/bin/zsh
RUN usermod --shell /bin/zsh root
RUN echo -e "alias :q='exit'\nalias :qa='exit'\nalias ls='ls --color'\nalias l='ls -lh'\nalias ll='ls -lha'\nalias vi='nvim'" >> /root/.zshrc
# }}}
# {{{ Neovim setup
RUN dnf --color yes --refresh -y install \
  fd-find \
  # for TSInstall dockerfile
  libstdc++-devel \
  neovim \
  ripgrep
# ensure minimum setup in $HOME/.local/share/nvim:
RUN nvim --headless --cmd q
RUN mkdir -p /root/.config/nvim/ftplugin && mkdir -p /root/.config/nvim/lua/config
COPY java.lua /root/.config/nvim/ftplugin
COPY init.vim /root/.config/nvim/init.vim
COPY init.lua /root/.config/nvim/lua
COPY plugins.lua /root/.config/nvim/lua
COPY nvim-cmp.lua /root/.config/nvim/lua/config
COPY nvim-lspconfig.lua /root/.config/nvim/lua/config
RUN nvim --headless -c'source ~/.config/nvim/init.vim' -c'Lazy install' -c'qa'
RUN nvim --headless -c'TSInstallSync java' -c'q'
COPY eclipse_plugin_builders.py /opt/eclipse_plugin_builders.py
RUN python3 -m venv /root/.venv && /root/.venv/bin/pip install click lxml
# }}}
# {{{ java-debug
WORKDIR /opt
RUN curl -L -o java-debug.tar.gz "https://github.com/microsoft/java-debug/archive/refs/tags/0.52.0.tar.gz" && \
  tar xvzf java-debug.tar.gz && \
  rm java-debug.tar.gz && \
  mv java-debug-* java-debug
WORKDIR /opt/java-debug
RUN zsh -c "./mvnw clean install" && chown -R root:root . && \
  find . -name "com.microsoft.java.debug.plugin-*.jar" -exec mv {} /opt \; && \
  rm -rf /opt/java-debug
# }}}
# {{{ Eclipse JDT Language Server
RUN mkdir /opt/eclipse.jdt.ls
WORKDIR /opt/eclipse.jdt.ls
RUN curl -L -o jdt-language-server.tar.gz \
  "https://www.eclipse.org/downloads/download.php?file=/jdtls/milestones/1.34.0/jdt-language-server-1.34.0-202404031240.tar.gz" && \
  tar xvzf jdt-language-server.tar.gz && \
  rm jdt-language-server.tar.gz && \
  chown -R root:root .
ENV PATH=/opt/eclipse.jdt.ls/bin:$PATH
# }}}
ENV DISPLAY="host.docker.internal:0.0"
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk
RUN mkdir -p /workspaces/RUNTIME && mkdir /root/projects
RUN echo -e "source /root/.venv/bin/activate" >> /root/.zshrc
WORKDIR /root/projects
ENTRYPOINT ["/bin/zsh"]
