let currentProduct=null;
let currentUser=localStorage.getItem("warehouseUser")||"";

function showLogin(){
  document.getElementById("loginPage").classList.remove("hidden");
  document.getElementById("registerPage").classList.add("hidden");
  document.getElementById("appPage").classList.add("hidden");
}

function showRegister(){
  document.getElementById("loginPage").classList.add("hidden");
  document.getElementById("registerPage").classList.remove("hidden");
  document.getElementById("appPage").classList.add("hidden");
}

function showApp(){
  document.getElementById("loginPage").classList.add("hidden");
  document.getElementById("registerPage").classList.add("hidden");
  document.getElementById("appPage").classList.remove("hidden");
  document.getElementById("welcomeUser").textContent="Logged in as: "+currentUser;
  document.getElementById("sideUser").textContent=currentUser;
  const initial=(currentUser||"U").charAt(0).toUpperCase();
  document.getElementById("userAvatar").textContent=initial;
  document.getElementById("topAvatar").textContent=initial;
  loadProducts();
}

function registerUser(){
  const u=encodeURIComponent(document.getElementById("registerUsername").value.trim());
  const p=document.getElementById("registerPassword").value;
  const c=document.getElementById("registerConfirm").value;

  if(!u || !p || !c){alert("Please fill in all fields.");return;}
  if(p!==c){alert("Passwords do not match.");return;}

  fetch(`/api/register?username=${u}&password=${encodeURIComponent(p)}`)
    .then(r=>r.json())
    .then(x=>{
      if(x.success){
        alert("Registration successful. You can now log in.");
        document.getElementById("loginUsername").value=decodeURIComponent(u);
        document.getElementById("loginPassword").value="";
        document.getElementById("registerUsername").value="";
        document.getElementById("registerPassword").value="";
        document.getElementById("registerConfirm").value="";
        showLogin();
      }else{
        alert(x.message||"Registration failed.");
      }
    })
    .catch(()=>alert("Could not connect to the server."));
}

function login(){
  const u=document.getElementById("loginUsername").value.trim();
  const p=document.getElementById("loginPassword").value;

  if(!u||!p){alert("Please enter your username and password.");return;}

  fetch(`/api/login?username=${encodeURIComponent(u)}&password=${encodeURIComponent(p)}`)
    .then(r=>r.json())
    .then(x=>{
      if(x.success){
        currentUser=u;
        localStorage.setItem("warehouseUser",u);
        showApp();
      }else{
        alert("Wrong username or password.");
      }
    })
    .catch(()=>alert("Could not connect to the server."));
}

function logout(){
  currentUser="";
  localStorage.removeItem("warehouseUser");
  currentProduct=null;
  document.getElementById("loginPassword").value="";
  showLogin();
}

function loadProducts(){
  fetch("/api/products").then(r=>r.json()).then(data=>{
    const box=document.getElementById("products");
    box.innerHTML="";

    updateStats(data);
    if(!data.length){
      box.innerHTML='<div class="empty">No products yet.<br>Add your first product above.</div>';
      return;
    }

    window.inventoryProducts=data;
    data.forEach(p=>{
      const d=document.createElement("div");
      d.className="product";
      d.innerHTML=`<b>${escapeHtml(p.name)}</b><span class="stock">Stock: ${p.stock}</span>`;
      d.onclick=()=>openStack(p);
      box.appendChild(d);
    });
  });
}

function updateStats(data){
  const products=Array.isArray(data)?data:[];
  document.getElementById("productCount").textContent=products.length;
  document.getElementById("stockCount").textContent=products.reduce((sum,p)=>sum+(Number(p.stock)||0),0);
  document.getElementById("batchCount").textContent=products.reduce((sum,p)=>sum+(Number(p.batches)||0),0) || products.filter(p=>(Number(p.stock)||0)>0).length;
}

function filterProducts(){
  const term=(document.getElementById("productSearch").value||"").trim().toLowerCase();
  document.querySelectorAll("#products .product").forEach(card=>{
    card.style.display=card.textContent.toLowerCase().includes(term)?"":"none";
  });
}

function addProduct(){
  const name=document.getElementById("newProduct").value.trim();
  if(!name){alert("Enter a product name.");return;}

  fetch(`/api/add-product?name=${encodeURIComponent(name)}`)
    .then(r=>r.json())
    .then(x=>{
      if(x.success){
        document.getElementById("newProduct").value="";
        loadProducts();
      }else{
        alert(x.message||"Could not add product.");
      }
    });
}

function openStack(p){
  currentProduct=p;
  document.getElementById("productsView").classList.add("hidden");
  document.getElementById("stackView").classList.remove("hidden");
  document.getElementById("stackTitle").textContent=p.name.toUpperCase()+" STACK";
  document.getElementById("message").textContent="";
  loadStack();
}

function showProducts(){
  document.getElementById("stackView").classList.add("hidden");
  document.getElementById("productsView").classList.remove("hidden");
  loadProducts();
}

function loadStack(){
  fetch(`/api/stack?product_id=${currentProduct.id}`).then(r=>r.json()).then(data=>{
    const box=document.getElementById("stack");
    box.innerHTML="";
    data.forEach((x,i)=>{
      let d=document.createElement("div");
      d.className="box"+(i===0?" top":"");
      d.innerHTML=`<strong>${escapeHtml(x.box)}</strong><span>Quantity: ${x.quantity}</span>`;
      box.appendChild(d);
    });
    if(!data.length)box.innerHTML="<p>No stock in this stack.</p>";
  });
}

function pushStock(){
  const q=document.getElementById("qty").value;
  const box=document.getElementById("box").value||("BOX-"+Date.now());

  fetch(`/api/push?product_id=${currentProduct.id}&quantity=${encodeURIComponent(q)}&box=${encodeURIComponent(box)}`)
    .then(r=>r.json())
    .then(x=>{
      if(x.success){
        msg("New stock added to the top of the stack.");
        document.getElementById("box").value="";
        document.getElementById("qty").value=1;
        loadStack();
      }else{
        msg(x.message||"Could not add stock.");
      }
    });
}

function popStock(){
  const q=document.getElementById("qty").value||1;
  fetch(`/api/pop?product_id=${currentProduct.id}&quantity=${encodeURIComponent(q)}`)
    .then(r=>r.json())
    .then(x=>{
      msg(x.message||"Stock updated.");
      loadStack();
    });
}

function msg(t){document.getElementById("message").textContent=t;}

function escapeHtml(text){
  const div=document.createElement("div");
  div.textContent=text;
  return div.innerHTML;
}

document.addEventListener("DOMContentLoaded",()=>{
  // Always show login when the website is opened.
  // The username is remembered only as a convenience; the password is never stored.
  showLogin();
  if(currentUser) document.getElementById("loginUsername").value=currentUser;
});
