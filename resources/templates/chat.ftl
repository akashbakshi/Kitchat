<html>
    <head>
        <link rel="stylesheet" href="/static/css/bootstrap.min.css"/>
        <link rel="stylesheet" href="/static/css/mdb.min.css"/>

        <script>
            let ws = new WebSocket("ws://localhost:5000/chat");

            ws.onopen = function(e){
                //ws.send(${room.username}+" has connected");
            };

            ws.onmessage = function(event){
                console.log(event);
            };

            function sendMessage(){
                let msg = document.getElementById('msg').value;
                let author = document.getElementById('username').value;

                ws.send(JSON.stringify({username:author,content:msg,roomName:"${room.name}"}));
            }
        </script>
        <title>KitChat - ${room.name}</title>
    </head>

    <body>
        <script type="text/javascript" src="/static/js/jquery.js"></script>
        <script type="text/javascript" src="/static/js/popper.js"></script>
        <script type="text/javascript" src="/static/js/bootstrap.js"></script>
        <script type="text/javascript" src="/static/js/mdb.js"></script>

        <div class="container justify-content-center">
            <h2 class="font-weight-bold my-5">Chat Room - ${room.name}</h2>

        <div class="my-5">

        </div>
        <form class="my-5" >
            <div class="form-group">

                <textarea id="msg" name="msg" rows="5" cols="100"></textarea>
            </div>

            <div class="form-group">
                <p>Chatting as ${room.username}</p>
                <input hidden id="username" name="username" value=${room.username} />
            </div>
            <div class="form-group">
                <button type="button" onclick="sendMessage()" class="btn btn-primary">Send</button>
            </div>
        </form>
        </div>
    </body>
</html>