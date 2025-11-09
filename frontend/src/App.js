import React from 'react';
import { BrowserRouter as Router, Route, Switch } from 'react-router-dom';
import Login from './components/Login';
import Signup from './components/Signup';
import VerifyEmail from './components/VerifyEmail';
import ResendVerification from './components/ResendVerification';
import ChangePassword from './components/ChangePassword';
import PostList from './components/PostList';
import PostDetail from './components/PostDetail';
import PostWrite from './components/PostWrite';
import PostEdit from './components/PostEdit';

function App() {
  return (
    <Router>
      <Switch>
        <Route exact path="/" component={Login} />
        <Route path="/signup" component={Signup} />
        <Route path="/verify-email" component={VerifyEmail} />
        <Route path="/resend-verification" component={ResendVerification} />
        <Route path="/change-password" component={ChangePassword} />
        <Route path="/posts" exact component={PostList} />
        <Route path="/posts/new" component={PostWrite} />
        <Route path="/posts/:id/edit" component={PostEdit} />
        <Route path="/posts/:id" component={PostDetail} />
      </Switch>
    </Router>
  );
}

export default App;
