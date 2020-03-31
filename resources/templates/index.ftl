
<html>
    <head>
        <link rel="stylesheet" href="/static/css/bootstrap.min.css"/>
        <link rel="stylesheet" href="/static/css/mdb.min.css"/>

        <title>KitChat Homepage</title>
    </head>
    <body>
        <#include "menu.ftl">
        <div class="container">
            <h1 class="text-center my-3 font-weight-bold">Join A Chat Room</h1>
            <form class="my-5" method="post" action="/enterchat">
                <div class="form-group">

                    <input class="form-control" type="text" id="username" name="username" placeholder="Username"/>
                </div>

                <div class="form-group">

                    <input class="form-control" type="text" id="roomName" name="roomName" placeholder="Chat Room Name"/>
                </div>
                <div class="form-group">
                    <button class="btn btn-brown" type="submit">Enter Chat</button>
                </div>
            </form>

            <hr class="hr-dark"/>
            <form class="my-5" method="post" action="/enterchat">
                <div class="form-group">

                    <input class="form-control" type="text" id="username" name="username" placeholder="Username"/>
                </div>

                <div class="form-group">
                    <button class="btn btn-black" type="submit">Chat Roulette</button>
                </div>
            </form>
        </div>

    </body>
</html>
