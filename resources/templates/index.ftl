<#-- @ftlvariable name="data" type="com.akashbakshi.IndexData" -->
<html>
    <head>
        <link rel="stylesheet" href="/static/css/bootstrap.min.css"/>
        <link rel="stylesheet" href="/static/css/mdb.min.css"/>

        <title>KitChat Homepage</title>
    </head>
    <body>
        <script type="text/javascript" src="/static/js/jquery.js"></script>
        <script type="text/javascript" src="/static/js/popper.js"></script>
        <script type="text/javascript" src="/static/js/bootstrap.js"></script>
        <script type="text/javascript" src="/static/js/mdb.js"></script>

        <div class="container">
            <h1 class="text-center my-3 font-weight-bold">Chat Info</h1>
            <form class="my-5" method="post" action="/enterchat">
                <div class="form-group">

                    <input class="form-control" type="text" id="username" name="username" placeholder="Username"/>
                </div>

                <div class="form-group">

                    <input class="form-control" type="text" id="roomName" name="roomName" placeholder="Chat Room Name"/>
                </div>
                <div class="form-group">
                    <button class="btn btn-primary" type="submit">Enter Chat</button>
                </div>
            </form>
        </div>

    </body>
</html>
