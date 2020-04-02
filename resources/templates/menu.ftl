<html>
    <head>
        <link rel="stylesheet" href="/static/css/bootstrap.min.css"/>
        <link rel="stylesheet" href="/static/css/mdb.min.css"/>
        <link rel="stylesheet" href="/static/css/style.css"/>
    </head>

    <body>

        <script type="text/javascript" src="/static/js/jquery.js"></script>
        <script type="text/javascript" src="/static/js/popper.js"></script>
        <script type="text/javascript" src="/static/js/bootstrap.js"></script>
        <script type="text/javascript" src="/static/js/mdb.js"></script>

        <nav class="navbar navbar-expand-lg navbar-dark danger-color-dark">
            <a class="navbar-brand" href="/">Kit Chat</a>
            <ul class="navbar-nav ml-auto nav-flex-auto">
                <li class="nav-item dropdown">
                    <a class="nav-link dropdown-toggle" id="userNavOptions" data-toggle="dropdown"
                       aria-haspopup="true" aria-expanded="false">
                        <i class="fas fa-user"></i>
                    </a>
                    <div class="dropdown-menu dropdown-menu-right dropdown-default"
                         aria-labelledby="userNavOptions">
                        <#if (user??)>
                            <a class="dropdown-item" href="/logout">Logout</a>
                        <#else>
                            <a class="dropdown-item" href="/login">Login</a>
                            <a class="dropdown-item" href="/signup">Sign Up</a>
                        </#if>
                    </div>
                </li>
            </ul>

        </nav>
    </body>

</html>